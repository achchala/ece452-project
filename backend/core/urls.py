"""URL configuration for the Django project."""

from django.urls import path, include
from rest_framework.routers import DefaultRouter
from core.views.auth import AuthView
from core.views.hello_world import HelloWorldView, DatabaseCheckView
from core.views.friend_request import FriendRequestView
from core.views.dashboard import DashboardView
from core.views.groups import GroupsView
from core.views.expenses import ExpensesView
from core.views.notifications import NotificationsView

# Create a router and register our viewsets with it
router = DefaultRouter()
router.register("friend", FriendRequestView, basename="friend")
router.register("auth", AuthView, basename="auth")
router.register("dashboard", DashboardView, basename="dashboard")
router.register("groups", GroupsView, basename="groups")
router.register("expenses", ExpensesView, basename="expenses")
router.register("notifications", NotificationsView, basename="notifications")

# The API URLs are now determined automatically by the router
urlpatterns = [
    path("", HelloWorldView.as_view(), name="hello_world"),
    path("check-db", DatabaseCheckView.as_view(), name="check_db"),
    path("api/", include(router.urls)),
]
