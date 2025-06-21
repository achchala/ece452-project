"""Views for dashboard functionality."""

from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase


class DashboardView(viewsets.ViewSet):
    """ViewSet for dashboard functionality, using Supabase."""

    @action(detail=False, methods=["get"], url_path="user-expenses")
    def get_user_expenses(self, request):
        """Get dashboard data for a user including lent and owed amounts."""
        user_id = request.query_params.get("user_id")
        
        if not user_id:
            return Response(
                {"error": "user_id is required"}, status=status.HTTP_400_BAD_REQUEST
            )
        
        try:
            user_id = int(user_id)
        except ValueError:
            return Response(
                {"error": "user_id must be a valid integer"}, status=status.HTTP_400_BAD_REQUEST
            )

        dashboard_data = supabase.expenses.get_user_dashboard_data(user_id)
        
        if dashboard_data is None:
            return Response(
                {"error": "Failed to retrieve dashboard data"}, 
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
            
        return Response(dashboard_data)

    @action(detail=False, methods=["get"], url_path="lent")
    def get_lent_expenses(self, request):
        """Get all expenses where the user lent money."""
        user_id = request.query_params.get("user_id")
        
        if not user_id:
            return Response(
                {"error": "user_id is required"}, status=status.HTTP_400_BAD_REQUEST
            )
        
        try:
            user_id = int(user_id)
        except ValueError:
            return Response(
                {"error": "user_id must be a valid integer"}, status=status.HTTP_400_BAD_REQUEST
            )

        lent_expenses = supabase.expenses.get_user_lent_expenses(user_id)
        
        if lent_expenses is None:
            return Response(
                {"error": "Failed to retrieve lent expenses"}, 
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
            
        return Response({"lent_expenses": lent_expenses})

    @action(detail=False, methods=["get"], url_path="owed")
    def get_owed_splits(self, request):
        """Get all splits where the user owes money."""
        user_id = request.query_params.get("user_id")
        
        if not user_id:
            return Response(
                {"error": "user_id is required"}, status=status.HTTP_400_BAD_REQUEST
            )
        
        try:
            user_id = int(user_id)
        except ValueError:
            return Response(
                {"error": "user_id must be a valid integer"}, status=status.HTTP_400_BAD_REQUEST
            )

        owed_splits = supabase.expenses.get_user_owed_splits(user_id)
        
        if owed_splits is None:
            return Response(
                {"error": "Failed to retrieve owed splits"}, 
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
            
        return Response({"owed_splits": owed_splits}) 