"""URL configuration for the Django project."""

from django.urls import path, include
from rest_framework.routers import DefaultRouter
from core.views.hello_world import HelloWorldView, DatabaseCheckView
from core.views.friend_request import FriendRequestView

# Create a router and register our viewsets with it
router = DefaultRouter()
router.register("friend", FriendRequestView, basename="friend")

# The API URLs are now determined automatically by the router
urlpatterns = [
    path("", HelloWorldView.as_view(), name="hello_world"),
    path("check-db", DatabaseCheckView.as_view(), name="check_db"),
    path("api/", include(router.urls)),
]
