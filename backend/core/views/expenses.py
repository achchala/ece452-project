from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase


class ExpensesView(viewsets.ViewSet):
    """ViewSet for expense CRUD operations, using Supabase."""

    @action(detail=False, methods=["post"], url_path="create")
    def create_expense(self, request):
        """Create a new expense."""
        title = request.data.get("title")
        total_amount = request.data.get("totalAmount")
        firebase_id = request.data.get("firebaseId")
        splits = request.data.get("splits", [])

        if not all([title, total_amount, firebase_id]):
            return Response(
                {"error": "title, totalAmount, and firebaseId are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        created_by = user.get("id")
        
        # Create the expense
        group_id = request.data.get("groupId")
        expense = supabase.expenses.create_expense(title, total_amount, created_by, group_id)
        
        if expense is None:
            return Response(
                {"error": "Failed to create expense"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        expense_id = expense.get("id")
        
        # Create splits if provided
        created_splits = []
        if splits:
            for split in splits:
                user_email = split.get("userEmail")
                amount_owed = split.get("amountOwed")
                
                if user_email and amount_owed is not None:
                    # Get user by email
                    split_user = supabase.users.get_by_email(user_email)
                    if split_user:
                        split_data = supabase.expenses.create_split(
                            expense_id, 
                            split_user.get("id"), 
                            amount_owed
                        )
                        if split_data:
                            created_splits.append(split_data)

        return Response({
            "message": "Expense created successfully",
            "expense": {
                "id": expense.get("id"),
                "title": expense.get("title"),
                "total_amount": expense.get("total_amount"),
                "created_by": expense.get("created_by"),
                "created_at": expense.get("created_at"),
                "splits": created_splits
            }
        }, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["post"], url_path="user-expenses")
    def get_user_expenses(self, request):
        """Get all expenses for a user (both lent and owed)."""
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        user_id = user.get("id")
        
        # Get expenses where user lent money
        lent_expenses = supabase.expenses.get_user_lent_expenses(user_id) or []
        
        # Get splits where user owes money
        owed_splits = supabase.expenses.get_user_owed_splits(user_id) or []

        return Response({
            "lent_expenses": lent_expenses,
            "owed_splits": owed_splits
        })

    @action(detail=True, methods=["get"], url_path="detail")
    def get_expense(self, request, pk=None):
        """Get a specific expense by ID with all its splits."""
        try:
            expense_id = int(pk)
        except ValueError:
            return Response(
                {"error": "Invalid expense ID"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        expense = supabase.expenses.get_expense_with_splits(expense_id)

        if expense is None:
            return Response(
                {"error": "Expense not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        return Response(expense)

    @action(detail=True, methods=["put"], url_path="update")
    def update_expense(self, request, pk=None):
        """Update an expense."""
        try:
            expense_id = int(pk)
        except ValueError:
            return Response(
                {"error": "Invalid expense ID"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        title = request.data.get("title")
        total_amount = request.data.get("totalAmount")
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Check if user is the creator of the expense
        expense = supabase.expenses.get_expense_with_splits(expense_id)
        if not expense or expense.get("created_by") != user.get("id"):
            return Response(
                {"error": "Unauthorized to update this expense"},
                status=status.HTTP_403_FORBIDDEN,
            )

        update_data = {}
        if title is not None:
            update_data["title"] = title
        if total_amount is not None:
            update_data["total_amount"] = total_amount

        if not update_data:
            return Response(
                {"error": "No fields to update"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Update the expense
        updated_expense = supabase.expenses.update_expense(expense_id, update_data)

        if updated_expense is None:
            return Response(
                {"error": "Failed to update expense"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response(updated_expense)

    @action(detail=True, methods=["delete"], url_path="delete")
    def delete_expense(self, request, pk=None):
        """Delete an expense."""
        try:
            expense_id = int(pk)
        except ValueError:
            return Response(
                {"error": "Invalid expense ID"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Check if user is the creator of the expense
        expense = supabase.expenses.get_expense_with_splits(expense_id)
        if not expense or expense.get("created_by") != user.get("id"):
            return Response(
                {"error": "Unauthorized to delete this expense"},
                status=status.HTTP_403_FORBIDDEN,
            )

        success = supabase.expenses.delete_expense(expense_id)

        if not success:
            return Response(
                {"error": "Failed to delete expense"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({"message": "Expense deleted successfully"})

    @action(detail=True, methods=["post"], url_path="add-split")
    def add_split(self, request, pk=None):
        """Add a split to an expense."""
        try:
            expense_id = int(pk)
        except ValueError:
            return Response(
                {"error": "Invalid expense ID"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        firebase_id = request.data.get("firebaseId")
        user_email = request.data.get("userEmail")
        amount_owed = request.data.get("amountOwed")

        if not all([firebase_id, user_email, amount_owed]):
            return Response(
                {"error": "firebaseId, userEmail, and amountOwed are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Get split user by email
        split_user = supabase.users.get_by_email(user_email)
        if not split_user:
            return Response(
                {"error": "Split user not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        split = supabase.expenses.create_split(expense_id, split_user.get("id"), amount_owed)

        if split is None:
            return Response(
                {"error": "Failed to add split"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({"message": "Split added successfully", "split": split})

    @action(detail=False, methods=["post"], url_path="dashboard")
    def get_dashboard_data(self, request):
        """Get dashboard data for a user including lent and owed amounts."""
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        user_id = user.get("id")
        dashboard_data = supabase.expenses.get_user_dashboard_data(user_id)

        return Response(dashboard_data)

    @action(detail=False, methods=["post"], url_path="group-expenses")
    def get_group_expenses(self, request):
        """Get all expenses for a specific group."""
        group_id = request.data.get("groupId")

        if not group_id:
            return Response(
                {"error": "groupId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        expenses = supabase.expenses.get_group_expenses(group_id)

        return Response({
            "expenses": expenses
        })

    @action(detail=False, methods=["post"], url_path="user-group-expenses")
    def get_user_group_expenses(self, request):
        """Get all expenses for a user in a specific group."""
        firebase_id = request.data.get("firebaseId")
        group_id = request.data.get("groupId")

        if not all([firebase_id, group_id]):
            return Response(
                {"error": "firebaseId and groupId are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        user_id = user.get("id")
        expenses = supabase.expenses.get_user_group_expenses(user_id, group_id)

        return Response(expenses) 