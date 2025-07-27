from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase


class NotificationsView(viewsets.ViewSet):
    """ViewSet for notifications."""

    @action(detail=False, methods=["post"], url_path="get-all-notifications")
    def get_all_notifications(self, request):
        '''gets all unprocessed notifications for a given user.'''
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # # Get user by Firebase ID
        # user = supabase.users.get_by_firebase_id(firebase_id)
        # if not user:
        #     return Response(
        #         {"error": "User not found"},
        #         status=status.HTTP_200_OK,
        #     )

        # user_id = user.get("id")

        notifications = supabase.notifications.get_all_unprocessed_notifications(firebase_id)
        
        if not notifications:
            return Response([])
        
        return Response(notifications)

    @action(detail=False, methods=["post"], url_path="update-notification-processed")
    def update_notification_processed(self, request):
        '''updates a notification to processed'''
        notification_id = request.data.get("notificationId")

        if not notification_id:
            return Response(
                {"error": "notificationId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        notification = supabase.notifications.update_notification_processed(notification_id)

        return Response(notification)






