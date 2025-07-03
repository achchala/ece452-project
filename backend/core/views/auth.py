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

        new_user = supabase.users.create(email=email, firebase_id=firebase_id)
        
        if not new_user:
            return Response(
                {"error": "Failed to create user"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({
            "message": "User created successfully",
            "user": new_user
        }, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["post"], url_path="update-name")
    def update_name(self, request):
        """Update user's name in Supabase."""
        firebase_id = request.data.get("firebaseId")
        name = request.data.get("name")

        if not all([firebase_id, name]):
            return Response(
                {"error": "firebaseId and name are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
            
        # Check if user exists
        existing_user = supabase.users.get_by_firebase_id(firebase_id)
        if not existing_user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Update the user's name
        updated_user = supabase.users.update_name(firebase_id=firebase_id, name=name)
        
        if not updated_user:
            return Response(
                {"error": "Failed to update user name"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({
            "message": "Name updated successfully", 
            "user": updated_user
        }, status=status.HTTP_200_OK)
    
    @action(detail=False, methods=["post"], url_path="get-user")
    def get_user(self, request):
        """Get user information by Firebase ID."""
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
            
        # Get user information from Supabase
        user = supabase.users.get_by_firebase_id(firebase_id)
        
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )
        
        return Response({
            "user": user
        }, status=status.HTTP_200_OK)         

    @action(detail=False, methods=["post"], url_path="get-user-by-email")
    def get_user_by_email(self, request):
        """Get user information by email."""
        email = request.data.get("email")

        if not email:
            return Response(
                {"error": "email is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
            
        # Get user information from Supabase
        user = supabase.users.get_by_email(email)
        
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )
        
        return Response({
            "user": user
        }, status=status.HTTP_200_OK)         