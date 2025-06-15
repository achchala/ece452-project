"""Views for the core application."""

from django.http import HttpResponse
from django.views import View
from django.db import connection


class HelloWorldView(View):
    """View for returning a simple Hello, World! response."""

    def get(self, _request):
        """Handle GET requests."""
        return HttpResponse("Hello, World!")


class DatabaseCheckView(View):
    """View for checking database connection status."""

    def get(self, _request):
        """Handle GET requests."""
        try:
            connection.ensure_connection()
            return HttpResponse("Database connection successful!")
        except Exception as e:
            return HttpResponse(f"Database connection failed: {e}")
