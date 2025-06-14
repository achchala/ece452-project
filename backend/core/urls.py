"""URL configuration for the Django project."""
from django.urls import path
from django.http import HttpResponse
from django.db import connection

def hello_world(request):
    """Return a simple Hello, World! response."""
    return HttpResponse("Hello, World!")

def check_db(request):
    """Check database connection and return status."""
    try:
        connection.ensure_connection()
        return HttpResponse("Database connection successful!")
    except Exception as e:
        return HttpResponse(f"Database connection failed: {e}")

urlpatterns = [
    path('', hello_world),
    path('check-db', check_db),
] 