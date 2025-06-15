"""Views for friend requests."""

from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.models.friend import FriendRequest


class FriendRequestView(viewsets.ViewSet):
    """ViewSet for friend requests."""

    @action(detail=False, methods=["get"], url_path="get-all-requests")
    def get_all_requests(self, request):
        """Get all friend requests for a user."""
        username = request.query_params.get("username")
        print(username)
        if not username:
            print("hello")
            return Response(
                {"error": "Username required"}, status=status.HTTP_400_BAD_REQUEST
            )

        requests = FriendRequest.objects.filter(
            to_user=username, request_completed=False
        )
        return Response({"requests": list(requests.values("from_user", "created_at"))})

    @action(detail=False, methods=["post"], url_path="add-friend")
    def add_friend(self, request):
        """Send a friend request."""
        from_user = request.data.get("from_user")
        to_user = request.data.get("to_user")

        if not all([from_user, to_user]):
            return Response(
                {"error": "Missing user information"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        FriendRequest.objects.get_or_create(
            from_user=from_user, to_user=to_user, defaults={"request_completed": False}
        )
        return Response({"status": "request sent"})

    @action(detail=False, methods=["post"], url_path="accept-friend")
    def accept_friend(self, request):
        """Accept a friend request."""
        from_user = request.data.get("from_user")
        to_user = request.data.get("to_user")

        try:
            friend_request = FriendRequest.objects.get(
                from_user=from_user, to_user=to_user, request_completed=False
            )
            friend_request.request_completed = True
            friend_request.save()
            return Response({"status": "accepted"})
        except FriendRequest.DoesNotExist:
            return Response(
                {"error": "Request not found"}, status=status.HTTP_404_NOT_FOUND
            )

    @action(detail=False, methods=["post"], url_path="reject-friend")
    def reject_friend(self, request):
        """Reject a friend request."""
        from_user = request.data.get("from_user")
        to_user = request.data.get("to_user")

        try:
            friend_request = FriendRequest.objects.get(
                from_user=from_user, to_user=to_user, request_completed=False
            )
            friend_request.delete()
            return Response({"status": "rejected"})
        except FriendRequest.DoesNotExist:
            return Response(
                {"error": "Request not found"}, status=status.HTTP_404_NOT_FOUND
            )
