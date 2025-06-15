"""Friend-related models."""

from django.db import models


class FriendRequest(models.Model):
    """Model for friend requests."""

    from_user = models.CharField(max_length=100)
    to_user = models.CharField(max_length=100)
    request_completed = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "friend_requests"
        unique_together = ("from_user", "to_user")
