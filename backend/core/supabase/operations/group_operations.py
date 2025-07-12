from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class GroupOperations:
    """Handles all group-related database operations using the Supabase client."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.groups_table = self.client.get_table_name("groups")
        self.group_memberships_table = self.client.get_table_name("group_memberships")
    
    def create_group(self, name: str, description: str, created_by: str, total_budget: Optional[float] = None) -> Optional[Dict]:
        """Create a new group and add the creator as a member."""
        data = {
            "name": name,
            "description": description,
            "created_by": created_by
        }
        
        if total_budget is not None:
            data["total_budget"] = total_budget
        
        group = self.client._execute_query(
            table_name=self.groups_table,
            operation='insert',
            data=data
        )
        
        if group:
            # Add the creator as a member of the group
            membership_data = {
                "group_id": group["id"],
                "user_id": created_by
            }
            self.client._execute_query(
                table_name=self.group_memberships_table,
                operation='insert',
                data=membership_data
            )
        
        return group
    
    def get_group_by_id(self, group_id: str) -> Optional[Dict]:
        """Get group by ID."""
        result = self.client._execute_query(
            table_name=self.groups_table,
            operation='select',
            filters={'id': group_id}
        )
        # Return the first item since we're querying by unique ID
        return result[0] if result else None
    
    def get_user_groups(self, user_id: str) -> Optional[List[Dict]]:
        """Get all groups that a user is a member of."""
        # First get the group memberships for the user
        memberships = self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='select',
            filters={'user_id': user_id}
        )
        
        if not memberships:
            return []
        
        # Then get the actual group data for each membership
        groups = []
        for membership in memberships:
            group_id = membership.get('group_id')
            if group_id:
                group = self.get_group_by_id(group_id)
                if group:
                    groups.append(group)
        
        return groups
    
    def get_group_members(self, group_id: str) -> Optional[List[Dict]]:
        """Get all members of a group with user information."""
        memberships = self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='select',
            filters={'group_id': group_id}
        )
        
        if not memberships:
            return []
        
        # Get user information for each membership
        members_with_users = []
        for membership in memberships:
            user_id = membership.get('user_id')
            if user_id:
                user = self.client._execute_query(
                    table_name=self.client.get_table_name("users"),
                    operation='select',
                    filters={'id': user_id}
                )
                user_data = user[0] if user else None
                
                member_data = {
                    'id': membership.get('id'),
                    'user_id': membership.get('user_id'),
                    'group_id': membership.get('group_id'),
                    'joined_at': membership.get('joined_at'),
                    'user': user_data
                }
                members_with_users.append(member_data)
        
        return members_with_users
    
    def add_member_to_group(self, group_id: str, user_id: str) -> Optional[Dict]:
        """Add a user to a group."""
        # Only include the required fields, let the database handle defaults
        data = {
            "group_id": group_id,
            "user_id": user_id
        }
        
        # Check if user is already a member of the group
        existing_membership = self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='select',
            filters={'group_id': group_id, 'user_id': user_id}
        )
        
        if existing_membership:
            # User is already a member, return the existing membership
            return existing_membership[0]
        
        # Add the new membership
        return self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='insert',
            data=data
        )
    
    def remove_member_from_group(self, group_id: str, user_id: str) -> bool:
        """Remove a user from a group."""
        return self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='delete',
            filters={'group_id': group_id, 'user_id': user_id}
        )
    
    def update_group(self, group_id: str, data: Dict[str, Any]) -> Optional[Dict]:
        """Update group information."""
        return self.client._execute_query(
            table_name=self.groups_table,
            operation='update',
            data=data,
            filters={'id': group_id}
        )
    
    def delete_group(self, group_id: str) -> bool:
        """Delete a group and all its memberships."""
        # First delete all memberships
        self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='delete',
            filters={'group_id': group_id}
        )
        
        # Then delete the group
        return self.client._execute_query(
            table_name=self.groups_table,
            operation='delete',
            filters={'id': group_id}
        )
    
    def is_user_member_of_group(self, group_id: str, user_id: str) -> bool:
        """Check if a user is a member of a group."""
        membership = self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='select',
            filters={'group_id': group_id, 'user_id': user_id}
        )
        return bool(membership)
    
    def get_groups_created_by_user(self, user_id: str) -> Optional[List[Dict]]:
        """Get all groups created by a specific user."""
        result = self.client._execute_query(
            table_name=self.groups_table,
            operation='select',
            filters={'created_by': user_id}
        )
        return result if result else [] 