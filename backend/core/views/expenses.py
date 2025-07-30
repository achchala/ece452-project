from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase
from core.supabase.operations.credit_score_operations import CreditScoreOperations


class ExpensesView(viewsets.ViewSet):
    """ViewSet for expense CRUD operations, using Supabase."""

    @action(detail=False, methods=["post"], url_path="create")
    def create_expense(self, request):
        """Create a new expense."""
        title = request.data.get("title")
        total_amount = request.data.get("totalAmount")
        firebase_id = request.data.get("firebaseId")
        splits = request.data.get("splits", [])
        due_date = request.data.get("dueDate")
        category = request.data.get("category")

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
        expense = supabase.expenses.create_expense(
            title, total_amount, created_by, group_id, due_date, category
        )

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
                        # Handle negative amounts (credits for expense creator)
                        # If amount is negative, it means the user is getting credited for their portion
                        # We store the absolute value but mark it as a credit
                        actual_amount = abs(amount_owed)
                        split_data = supabase.expenses.create_split(
                            expense_id, split_user.get("id"), actual_amount
                        )
                        if split_data:
                            # If this is a credit (negative amount), mark it as paid
                            if amount_owed < 0:
                                # Mark the split as paid since the creator is getting credited
                                supabase.expenses.confirm_payment(split_data.get("id"), created_by)
                            created_splits.append(split_data)

        # Update group budget if group_id is provided
        if group_id:
            supabase.expenses.update_group_budget_after_expense(group_id, total_amount)

        # Update credit scores for all users involved in the expense
        try:
            credit_score_ops = CreditScoreOperations(supabase.base_client)
            # Update credit score for the expense creator
            credit_score_ops.update_user_credit_score(created_by)
            
            # Update credit scores for all users who owe money
            for split in created_splits:
                if split and split.get('userid'):
                    credit_score_ops.update_user_credit_score(split.get('userid'))
        except Exception as e:
            # Log error but don't fail the expense creation
            print(f"Credit score update failed: {e}")

        return Response(
            {
                "message": "Expense created successfully",
                "expense": {
                    "id": expense.get("id"),
                    "title": expense.get("title"),
                    "total_amount": expense.get("total_amount"),
                    "due_date": expense.get("due_date"),
                    "category": expense.get("category"),
                    "created_by": expense.get("created_by"),
                    "created_at": expense.get("created_at"),
                    "splits": created_splits,
                },
            },
            status=status.HTTP_201_CREATED,
        )

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

        return Response({"lent_expenses": lent_expenses, "owed_splits": owed_splits})

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
        expense_id = pk  # Keep as string since it's a UUID

        title = request.data.get("title")
        total_amount = request.data.get("totalAmount")
        firebase_id = request.data.get("firebaseId")
        category = request.data.get("category")
        due_date = request.data.get("dueDate")

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
        if not expense:
            return Response(
                {"error": "Expense not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Debug: Print the IDs being compared
        print(
            f"DEBUG: expense.created_by = {expense.get('created_by')} (type: {type(expense.get('created_by'))})"
        )
        print(f"DEBUG: user.id = {user.get('id')} (type: {type(user.get('id'))})")

        if expense.get("created_by") != user.get("id"):
            return Response(
                {"error": "Unauthorized to update this expense"},
                status=status.HTTP_403_FORBIDDEN,
            )

        update_data = {}
        if title is not None:
            update_data["title"] = title
        if total_amount is not None:
            update_data["total_amount"] = total_amount
        if category is not None:
            update_data["category"] = category
        if due_date is not None:
            update_data["due_date"] = due_date

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
        expense_id = pk  # Keep as string since it's a UUID

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

        split = supabase.expenses.create_split(
            expense_id, split_user.get("id"), amount_owed
        )

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

        return Response({"expenses": expenses})

    @action(detail=False, methods=["post"], url_path="expense-notification")
    def post_expense_notification(self, request):
        group_id = request.data.get("groupId")
        expense_title = request.data.get("expenseTitle")

        group_members = supabase.groups.get_group_members(group_id)
        group = supabase.groups.get_group_by_id(group_id)

        if not group:
            return Response(
                {"error": "Failed to get group name"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        for member in group_members:
            user_id = member.get("user_id")
            notification = supabase.notifications.insert_notification(
                user_id,
                "A new expense '"
                + expense_title
                + "' has been added to "
                + group.get("name"),
                False,
            )

            if not notification:
                return Response(
                    {"error": "Failed to add notification"},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR,
                )

        return Response(notification)

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

    @action(detail=False, methods=["post"], url_path="request-payment")
    def request_payment_confirmation(self, request):
        """Request payment confirmation for a split."""
        split_id = request.data.get("split_id")
        user_id = request.data.get("user_id")
        
        if not all([split_id, user_id]):
            return Response(
                {"error": "split_id and user_id are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        result = supabase.expenses.request_payment_confirmation(split_id, user_id)
        
        if result is None:
            return Response(
                {"error": "Failed to request payment confirmation or split not found"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        # Update credit score for the user who requested payment
        try:
            credit_score_ops = CreditScoreOperations(supabase.base_client)
            credit_score_ops.update_user_credit_score(user_id)
        except Exception as e:
            # Log error but don't fail the payment request
            print(f"Credit score update failed: {e}")
        
        return Response({"message": "Payment confirmation requested", "split": result})

    @action(detail=False, methods=["post"], url_path="confirm-payment")
    def confirm_payment(self, request):
        """Confirm payment for a split."""
        split_id = request.data.get("split_id")
        lender_id = request.data.get("lender_id")
        
        if not all([split_id, lender_id]):
            return Response(
                {"error": "split_id and lender_id are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        result = supabase.expenses.confirm_payment(split_id, lender_id)
        
        if result is None:
            return Response(
                {"error": "Failed to confirm payment or unauthorized"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        # Update credit score for the user who paid
        try:
            credit_score_ops = CreditScoreOperations(supabase.base_client)
            # Get the user ID from the split
            split_data = supabase.base_client._execute_query(
                table_name=supabase.base_client.get_table_name("splits"),
                operation='select',
                filters={'id': split_id}
            )
            
            if split_data:
                user_id = split_data[0].get('userid')
                if user_id:
                    credit_score_ops.update_user_credit_score(user_id)
        except Exception as e:
            # Log error but don't fail the payment confirmation
            print(f"Credit score update failed: {e}")
        
        return Response({"message": "Payment confirmed", "split": result})

    @action(detail=False, methods=["post"], url_path="reject-payment")
    def reject_payment(self, request):
        """Reject payment for a split."""
        split_id = request.data.get("split_id")
        lender_id = request.data.get("lender_id")
        
        if not all([split_id, lender_id]):
            return Response(
                {"error": "split_id and lender_id are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        result = supabase.expenses.reject_payment(split_id, lender_id)
        
        if result is None:
            return Response(
                {"error": "Failed to reject payment or unauthorized"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        # Update credit score for the user whose payment was rejected
        try:
            credit_score_ops = CreditScoreOperations(supabase.base_client)
            # Get the user ID from the split
            split_data = supabase.base_client._execute_query(
                table_name=supabase.base_client.get_table_name("splits"),
                operation='select',
                filters={'id': split_id}
            )
            
            if split_data:
                user_id = split_data[0].get('userid')
                if user_id:
                    credit_score_ops.update_user_credit_score(user_id)
        except Exception as e:
            # Log error but don't fail the payment rejection
            print(f"Credit score update failed: {e}")
        
        return Response({"message": "Payment rejected", "split": result})

    @action(detail=False, methods=["get"], url_path="pending-payments")
    def get_pending_payment_requests(self, request):
        """Get pending payment requests for a lender."""
        lender_id = request.query_params.get("lender_id")
        
        if not lender_id:
            return Response(
                {"error": "lender_id is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        pending_requests = supabase.expenses.get_pending_payment_requests(lender_id)
        
        return Response({"pending_requests": pending_requests})
