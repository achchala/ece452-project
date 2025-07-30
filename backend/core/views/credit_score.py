from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase
from core.supabase.operations.credit_score_operations import CreditScoreOperations


class CreditScoreView(viewsets.ViewSet):
    """ViewSet for credit score operations."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.credit_score_ops = CreditScoreOperations(supabase)

    @action(detail=False, methods=["get"], url_path="user/(?P<user_id>[^/.]+)")
    def get_user_credit_score(self, request, user_id=None):
        """Get credit score for a specific user."""
        if not user_id:
            return Response(
                {"error": "user_id is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        result = self.credit_score_ops.get_user_credit_score(user_id)
        
        if result is None:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )
        
        return Response(result, status=status.HTTP_200_OK)

    @action(detail=False, methods=["post"], url_path="calculate/(?P<user_id>[^/.]+)")
    def calculate_user_credit_score(self, request, user_id=None):
        """Calculate and update credit score for a specific user."""
        if not user_id:
            return Response(
                {"error": "user_id is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        result = self.credit_score_ops.update_user_credit_score(user_id)
        
        if result is None:
            return Response(
                {"error": "Failed to calculate credit score"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        
        return Response(result, status=status.HTTP_200_OK)

    @action(detail=False, methods=["post"], url_path="calculate-all")
    def calculate_all_credit_scores(self, request):
        """Calculate credit scores for all users."""
        result = self.credit_score_ops.update_all_credit_scores()
        
        return Response(result, status=status.HTTP_200_OK)

    @action(detail=False, methods=["get"], url_path="leaderboard")
    def get_credit_score_leaderboard(self, request):
        """Get credit score leaderboard (top users by credit score)."""
        limit = request.query_params.get('limit', 10)
        try:
            limit = int(limit)
        except ValueError:
            limit = 10
        
        # Get users with credit scores, ordered by score descending
        users = supabase._execute_query(
            table_name=supabase.get_table_name("users"),
            operation='select',
            filters={'credit_score__not': None},
            order_by={'credit_score': 'desc'},
            limit=limit
        ) or []
        
        leaderboard = []
        for i, user in enumerate(users, 1):
            leaderboard.append({
                'rank': i,
                'user_id': user.get('id'),
                'name': user.get('name', 'Unknown'),
                'email': user.get('email'),
                'credit_score': user.get('credit_score')
            })
        
        return Response({
            'leaderboard': leaderboard,
            'total_users': len(leaderboard)
        }, status=status.HTTP_200_OK)

    @action(detail=False, methods=["get"], url_path="stats")
    def get_credit_score_stats(self, request):
        """Get credit score statistics."""
        # Get all users with credit scores
        users_with_scores = supabase._execute_query(
            table_name=supabase.get_table_name("users"),
            operation='select',
            filters={'credit_score__not': None}
        ) or []
        
        # Get all users
        all_users = supabase._execute_query(
            table_name=supabase.get_table_name("users"),
            operation='select'
        ) or []
        
        if not users_with_scores:
            return Response({
                'total_users': len(all_users),
                'users_with_scores': 0,
                'users_without_scores': len(all_users),
                'average_score': None,
                'score_ranges': {
                    'excellent': 0,
                    'good': 0,
                    'fair': 0,
                    'poor': 0
                }
            }, status=status.HTTP_200_OK)
        
        # Calculate statistics
        scores = [user.get('credit_score') for user in users_with_scores]
        average_score = sum(scores) / len(scores)
        
        # Categorize scores
        excellent = len([s for s in scores if s >= 750])
        good = len([s for s in scores if 650 <= s < 750])
        fair = len([s for s in scores if 550 <= s < 650])
        poor = len([s for s in scores if s < 550])
        
        return Response({
            'total_users': len(all_users),
            'users_with_scores': len(users_with_scores),
            'users_without_scores': len(all_users) - len(users_with_scores),
            'average_score': round(average_score, 2),
            'score_ranges': {
                'excellent': excellent,
                'good': good,
                'fair': fair,
                'poor': poor
            }
        }, status=status.HTTP_200_OK) 