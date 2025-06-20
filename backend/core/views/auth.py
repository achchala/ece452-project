from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase

class AuthView(viewsets.ViewSet):
    """ViewSet for user authentication, using Supabase."""

    @action(detail=False, methods=["post"], url_path="register")
    def register(self, request):
        """Register a new user in Supabase."""
        email = request.data.get("email")
        firebase_id = request.data.get("firebaseId")

        if not all([email, firebase_id]):
            return Response(
                {"error": "Email and firebaseId are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
            
        # Check if user already exists
        existing_user = supabase.users.get_by_firebase_id(firebase_id)
        if existing_user:
            return Response(
                {"error": "User with this Firebase ID already exists"},
                status=status.HTTP_409_CONFLICT,
            )

        # Create the new user
        new_user = supabase.users.create(email=email, firebase_id=firebase_id)

        if new_user:
            return Response({
                "message": "User created successfully", 
                "user": new_user
            }, status=status.HTTP_201_CREATED)
        else:
            return Response(
                {"error": "Failed to create user"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
            
            