"""Views for friend requests."""

from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase


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
