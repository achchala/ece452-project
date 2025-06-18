"""User model for the application."""

from django.db import models
from django.utils import timezone


class User(models.Model):
    """User model for storing user information."""
    id = models.AutoField(primary_key=True)
    email = models.EmailField(unique=True)
    date_joined = models.DateTimeField(default=timezone.now)
    firebase_id = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = "users"

    def __str__(self):
        return self.usernames
