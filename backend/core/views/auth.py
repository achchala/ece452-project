from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.models.user import User

class AuthView(viewsets.ViewSet):
    @action(detail=False, methods=["post"], url_path="register")
    def register(self, request):
        """Register a new user."""
        email = request.data.get("email")
        firebase_id = request.data.get("firebaseId")
        print(email, firebase_id)
        if not all([email, firebase_id]):
            return Response(
                {"error": "Email and password are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        user = User.objects.create(
            email=email,
            firebase_id=firebase_id,
        )
        return Response({
            "message": "user created", 
            "user": {
                "id": user.id,
                "email": user.email,
                "firebase_id": user.firebase_id
            }
        })
            
            