"""Views for friend requests."""

from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase
from core.supabase.operations.credit_score_operations import CreditScoreOperations
from core.supabase.operations.expense_operations import ExpenseOperations
from datetime import datetime, timedelta
from collections import defaultdict


def normalize_category_name(category):
    """Normalize category names to standard format matching frontend ExpenseCategory enum."""
    if not category:
        return "Other"
    
    # Convert to lowercase and strip whitespace
    normalized = category.lower().strip()
    
    # Map enum values to display names (from ExpenseCategory enum)
    enum_mapping = {
        "food_drinks": "Food & Drinks",
        "transport": "Transport", 
        "entertainment": "Entertainment",
        "shopping": "Shopping",
        "travel": "Travel",
        "utilities": "Utilities",
        "health": "Health",
        "education": "Education",
        "home": "Home",
        "work": "Work",
        "other": "Other",
    }
    
    # Check if it's an enum value first
    if normalized in enum_mapping:
        return enum_mapping[normalized]
    
    # Map common variations to standard names matching ExpenseCategory display names
    category_mapping = {
        # Food & Drinks
        "food": "Food & Drinks",
        "dining": "Food & Drinks",
        "restaurant": "Food & Drinks",
        "groceries": "Food & Drinks",
        "takeout": "Food & Drinks",
        "coffee": "Food & Drinks",
        "lunch": "Food & Drinks",
        "dinner": "Food & Drinks",
        "breakfast": "Food & Drinks",
        "drinks": "Food & Drinks",
        "food & drinks": "Food & Drinks",
        "food and drinks": "Food & Drinks",
        
        # Transport
        "transportation": "Transport",
        "uber": "Transport",
        "lyft": "Transport",
        "taxi": "Transport",
        "gas": "Transport",
        "fuel": "Transport",
        "parking": "Transport",
        "public transit": "Transport",
        "bus": "Transport",
        "subway": "Transport",
        "train": "Transport",
        "car": "Transport",
        "driving": "Transport",
        
        # Entertainment
        "movies": "Entertainment",
        "netflix": "Entertainment",
        "spotify": "Entertainment",
        "games": "Entertainment",
        "concert": "Entertainment",
        "theater": "Entertainment",
        "sports": "Entertainment",
        "activities": "Entertainment",
        "fun": "Entertainment",
        
        # Shopping
        "clothes": "Shopping",
        "clothing": "Shopping",
        "amazon": "Shopping",
        "online shopping": "Shopping",
        "retail": "Shopping",
        "electronics": "Shopping",
        "gifts": "Shopping",
        "purchase": "Shopping",
        
        # Travel
        "vacation": "Travel",
        "hotel": "Travel",
        "flight": "Travel",
        "airbnb": "Travel",
        "trip": "Travel",
        "holiday": "Travel",
        
        # Utilities
        "electricity": "Utilities",
        "water": "Utilities",
        "internet": "Utilities",
        "phone": "Utilities",
        "wifi": "Utilities",
        "cable": "Utilities",
        "bills": "Utilities",
        
        # Health
        "fitness": "Health",
        "gym": "Health",
        "medical": "Health",
        "pharmacy": "Health",
        "doctor": "Health",
        "wellness": "Health",
        "medicine": "Health",
        
        # Education
        "books": "Education",
        "tuition": "Education",
        "course": "Education",
        "school": "Education",
        "learning": "Education",
        "supplies": "Education",
        "study": "Education",
        
        # Home
        "garden": "Home",
        "furniture": "Home",
        "repair": "Home",
        "maintenance": "Home",
        "rent": "Home",
        "house": "Home",
        "apartment": "Home",
        
        # Work
        "business": "Work",
        "office": "Work",
        "job": "Work",
        "professional": "Work",
        "business expenses": "Work",
        "office supplies": "Work",
        
        # Other
        "misc": "Other",
        "miscellaneous": "Other",
        "unknown": "Other",
        "null": "Other",
        "none": "Other",
        "": "Other",
        "general": "Other",
        "personal": "Other",
        "uncategorized": "Other",
        "no category": "Other",
        "not specified": "Other",
    }
    
    return category_mapping.get(normalized, "Other")


class FriendRequestView(viewsets.ViewSet):
    """ViewSet for friend requests, using Supabase."""

    @action(detail=False, methods=["get"], url_path="get-all-requests")
    def get_all_requests(self, request):
        """Get all incoming, pending friend requests for a user."""
        to_user = request.query_params.get("username")
        if not to_user:
            return Response(
                {"error": "Username is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        requests = supabase.friend_requests.get_incoming(to_user)
        
        if requests is None:
            return Response({"error": "Failed to retrieve requests"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            
        return Response({"requests": requests})

    @action(detail=False, methods=["post"], url_path="add-friend")
    def add_friend(self, request):
        """Send a friend request."""
        from_user = request.data.get("from_user")
        to_user = request.data.get("to_user")

        if not all([from_user, to_user]):
            return Response(
                {"error": "Both from_user and to_user are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
            
        if from_user == to_user:
            return Response(
                {"error": "Cannot send a friend request to yourself"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        new_request = supabase.friend_requests.create(from_user=from_user, to_user=to_user)
        
        if new_request:
            return Response({"status": "request sent", "data": new_request}, status=status.HTTP_201_CREATED)
        else:
            # This could be due to a duplicate request or a database error.
            return Response({"error": "Failed to send request, it may already exist"}, status=status.HTTP_409_CONFLICT)

    @action(detail=False, methods=["post"], url_path="accept-friend")
    def accept_friend(self, request):
        """Accept a friend request."""
        from_user = request.data.get("from_user")
        to_user = request.data.get("to_user")
        
        if not all([from_user, to_user]):
            return Response(
                {"error": "Both from_user and to_user are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        success = supabase.friend_requests.accept(from_user=from_user, to_user=to_user)
        
        if success:
            return Response({"status": "accepted"})
        else:
            return Response(
                {"error": "Request not found or already accepted"}, status=status.HTTP_404_NOT_FOUND
            )

    @action(detail=False, methods=["post"], url_path="reject-friend")
    def reject_friend(self, request):
        """Reject a friend request."""
        from_user = request.data.get("from_user")
        to_user = request.data.get("to_user")
        
        if not all([from_user, to_user]):
            return Response(
                {"error": "Both from_user and to_user are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        success = supabase.friend_requests.reject(from_user=from_user, to_user=to_user)
        
        if success:
            return Response({"status": "rejected"})
        else:
            return Response(
                {"error": "Request not found"}, status=status.HTTP_404_NOT_FOUND
            )

    @action(detail=False, methods=["get"], url_path="get-outgoing-requests")
    def get_outgoing_requests(self, request):
        """Get all outgoing, pending friend requests from a user."""
        from_user = request.query_params.get("username")
        if not from_user:
            return Response(
                {"error": "Username is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        requests = supabase.friend_requests.get_outgoing(from_user)
        
        if requests is None:
            return Response({"error": "Failed to retrieve outgoing requests"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            
        return Response({"requests": requests})

    @action(detail=False, methods=["get"], url_path="get-friends")
    def get_friends(self, request):
        """Get all current friends for a user."""
        username = request.query_params.get("username")
        if not username:
            return Response(
                {"error": "Username is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        friends = supabase.friend_requests.get_friends(username)
        
        if friends is None:
            return Response({"error": "Failed to retrieve friends"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            
        return Response({"friends": friends})

    @action(detail=False, methods=["post"], url_path="friend-analytics")
    def get_friend_analytics(self, request):
        """Get friend analytics including user info, credit score, and spending analytics."""
        friend_email = request.data.get("friend_email")
        current_user_email = request.data.get("current_user_email")
        
        if not all([friend_email, current_user_email]):
            return Response(
                {"error": "Both friend_email and current_user_email are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            # Get friend user data
            friend_user = supabase.users.get_by_email(friend_email)
            if not friend_user:
                return Response(
                    {"error": "Friend user not found"},
                    status=status.HTTP_404_NOT_FOUND,
                )

            # Get friendship date
            friendship_data = supabase.friend_requests.get_friendship_date(current_user_email, friend_email)
            friendship_date = friendship_data.get("created_at") if friendship_data else None

            # Get credit score
            credit_score_ops = CreditScoreOperations(supabase.base_client)
            credit_score_data = credit_score_ops.get_user_credit_score(friend_user.get("id"))
            credit_score = credit_score_data.get("credit_score") if credit_score_data else None

            # Get spending analytics by category
            expense_ops = ExpenseOperations(supabase.base_client)
            user_expenses = expense_ops.get_user_expenses(friend_user.get("id"))
            
            # Calculate spending by category
            category_spending = defaultdict(float)
            total_spent = 0
            
            for expense in user_expenses or []:
                # Handle NULL values in the database column
                raw_category = expense.get("category")
                print(f"Raw category from database: '{raw_category}' (type: {type(raw_category)})")
                
                if raw_category is None:
                    normalized_category = "Other"
                    print(f"NULL category found -> using 'Other'")
                else:
                    # Normalize category names
                    normalized_category = normalize_category_name(raw_category)
                    print(f"Expense category: '{raw_category}' -> normalized to: '{normalized_category}'")
                
                amount = float(expense.get("total_amount", 0)) / 100.0  # Convert cents to dollars
                category_spending[normalized_category] += amount
                total_spent += amount

            # Convert to list format for frontend
            spending_analytics = []
            for category, amount in category_spending.items():
                percentage = (amount / total_spent * 100) if total_spent > 0 else 0
                spending_analytics.append({
                    "category": category,
                    "amount": amount,
                    "percentage": round(percentage, 1)
                })

            # Sort by amount descending
            spending_analytics.sort(key=lambda x: x["amount"], reverse=True)

            return Response({
                "user_info": {
                    "name": friend_user.get("name"),
                    "email": friend_user.get("email"),
                    "friendship_date": friendship_date
                },
                "credit_score": credit_score,
                "spending_analytics": spending_analytics,
                "total_spent": total_spent
            })

        except Exception as e:
            return Response(
                {"error": f"Failed to get friend analytics: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["post"], url_path="friend-request-notification")
    def friend_request_notification(self, request):
        """Send a friend request notification to a user."""
        from_user_email = request.data.get("from_user_email")
        to_user_email = request.data.get("to_user_email")

        if not all([from_user_email, to_user_email]):
            return Response(
                {"error": "Both from_user and to_user are required"},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        to_user = supabase.users.get_by_email(to_user_email)
        if not to_user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )
                
        notification = supabase.notifications.insert_notification(to_user.get("id"), "You have a new friend request from " + from_user_email, False)

        if not notification:
            return Response(
                {"error": "Failed to add notification"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
        
        return Response(notification)