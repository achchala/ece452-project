from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from core.supabase import supabase


class GroupsView(viewsets.ViewSet):
    """ViewSet for group CRUD operations, using Supabase."""

    @action(detail=False, methods=["post"], url_path="create")
    def create_group(self, request):
        """Create a new group."""
        name = request.data.get("name")
        description = request.data.get("description", "")
        firebase_id = request.data.get("firebaseId")

        if not all([name, firebase_id]):
            return Response(
                {"error": "Name and firebaseId are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        created_by = user.get("id")
        group = supabase.groups.create_group(name, description, created_by)

        if group is None:
            return Response(
                {"error": "Failed to create group"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({
            "message": "Group created successfully",
            "group": {
                "id": group.get("id"),
                "name": group.get("name"),
                "description": group.get("description"),
                "creator_id": group.get("created_by"),
                "created_at": group.get("created_at"),
            }
        }, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["post"], url_path="user-groups")
    def get_user_groups(self, request):
        """Get all groups for a user."""
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        user_id = user.get("id")
        groups = supabase.groups.get_user_groups(user_id)

        if groups is None:
            return Response(
                {"error": "Failed to retrieve groups"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        # Format groups to match frontend expectations
        formatted_groups = []
        for group in groups:
            # Get members for this group
            members = supabase.groups.get_group_members(group.get("id"))
            
            formatted_group = {
                "id": group.get("id"),
                "name": group.get("name"),
                "description": group.get("description"),
                "creator_id": group.get("created_by"),
                "created_at": group.get("created_at"),
                "members": members or []
            }
            formatted_groups.append(formatted_group)

        return Response(formatted_groups)

    @action(detail=True, methods=["get"], url_path="detail")
    def get_group(self, request, pk=None):
        """Get a specific group by ID."""
        if not pk:
            return Response(
                {"error": "Group ID is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        group_id = pk
        group = supabase.groups.get_group_by_id(group_id)

        if group is None:
            return Response(
                {"error": "Group not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Get members for this group
        members = supabase.groups.get_group_members(group_id)

        formatted_group = {
            "id": group.get("id"),
            "name": group.get("name"),
            "description": group.get("description"),
            "creator_id": group.get("created_by"),
            "created_at": group.get("created_at"),
            "members": members or []
        }

        return Response(formatted_group)

    @action(detail=True, methods=["put"], url_path="update")
    def update_group(self, request, pk=None):
        """Update a group."""
        if not pk:
            return Response(
                {"error": "Group ID is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        group_id = pk

        name = request.data.get("name")
        description = request.data.get("description")
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Check if user is the creator of the group
        group = supabase.groups.get_group_by_id(group_id)
        if not group or group.get("created_by") != user.get("id"):
            return Response(
                {"error": "Unauthorized to update this group"},
                status=status.HTTP_403_FORBIDDEN,
            )

        update_data = {}
        if name is not None:
            update_data["name"] = name
        if description is not None:
            update_data["description"] = description

        if not update_data:
            return Response(
                {"error": "No fields to update"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        updated_group = supabase.groups.update_group(group_id, update_data)

        if updated_group is None:
            return Response(
                {"error": "Failed to update group"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response(updated_group)

    @action(detail=True, methods=["delete"], url_path="delete")
    def delete_group(self, request, pk=None):
        """Delete a group."""
        if not pk:
            return Response(
                {"error": "Group ID is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        group_id = pk

        success = supabase.groups.delete_group(group_id)

        if not success:
            return Response(
                {"error": "Failed to delete group"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({"message": "Group deleted successfully"})

    @action(detail=True, methods=["post"], url_path="add-member")
    def add_member(self, request, pk=None):
        """Add a member to a group."""
        if not pk:
            return Response(
                {"error": "Group ID is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        group_id = pk

        firebase_id = request.data.get("firebaseId")
        member_email = request.data.get("memberEmail")

        if not all([firebase_id, member_email]):
            return Response(
                {"error": "firebaseId and memberEmail are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Get member by email
        member = supabase.users.get_by_email(member_email)
        if not member:
            return Response(
                {"error": "Member not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        membership = supabase.groups.add_member_to_group(group_id, member.get("id"))

        if membership is None:
            return Response(
                {"error": "Failed to add member to group"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({"message": "Member added successfully"})

    @action(detail=True, methods=["post"], url_path="remove-member")
    def remove_member(self, request, pk=None):
        """Remove a member from a group."""
        if not pk:
            return Response(
                {"error": "Group ID is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        group_id = pk

        firebase_id = request.data.get("firebaseId")
        member_email = request.data.get("memberEmail")

        if not all([firebase_id, member_email]):
            return Response(
                {"error": "firebaseId and memberEmail are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Get member by email
        member = supabase.users.get_by_email(member_email)
        if not member:
            return Response(
                {"error": "Member not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        success = supabase.groups.remove_member_from_group(group_id, member.get("id"))

        if not success:
            return Response(
                {"error": "Failed to remove member from group"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response({"message": "Member removed successfully"})

    @action(detail=True, methods=["get"], url_path="members")
    def get_members(self, request, pk=None):
        """Get all members of a group."""
        if not pk:
            return Response(
                {"error": "Group ID is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        group_id = pk

        members = supabase.groups.get_group_members(group_id)

        if members is None:
            return Response(
                {"error": "Failed to retrieve group members"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response(members)
    
    @action(detail=False, methods=["post"], url_path="group-notification")
    def post_group_notification(self, request):
        email = request.data.get("email")
        group_id = request.data.get("groupId")

        if not email:
            return Response(
                {"error": "email is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        user = supabase.users.get_by_email(email)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )
        
        group = supabase.groups.get_group_by_id(group_id)

        if not group:
            return Response({"error": "Failed to get group name"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        user_id = user.get("id")
        notification = supabase.notifications.insert_notification(user_id, "You have been added to a new group: " + group.get("name"), False)

        if not notification:
            return Response(
                {"error": "Failed to add notification"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        
        return Response(notification)

    @action(detail=False, methods=["post"], url_path="created-by-user")
    def get_groups_created_by_user(self, request):
        """Get all groups created by a specific user."""
        firebase_id = request.data.get("firebaseId")

        if not firebase_id:
            return Response(
                {"error": "firebaseId is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Get user by Firebase ID
        user = supabase.users.get_by_firebase_id(firebase_id)
        if not user:
            return Response(
                {"error": "User not found"},
                status=status.HTTP_404_NOT_FOUND,
            )

        user_id = user.get("id")
        groups = supabase.groups.get_groups_created_by_user(user_id)

        if groups is None:
            return Response(
                {"error": "Failed to retrieve groups"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response(groups) 