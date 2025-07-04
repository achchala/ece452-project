from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class GroupOperations:
    """Handles all group-related database operations using the Supabase client."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.groups_table = self.client.get_table_name("groups")
        self.group_memberships_table = self.client.get_table_name("group_memberships")
    
    def create_group(self, name: str, description: str, created_by: int) -> Optional[Dict]:
        """Create a new group and add the creator as a member."""
        data = {
            "name": name,
            "description": description,
            "created_by": created_by
        }
        
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
    
    def get_group_by_id(self, group_id: int) -> Optional[Dict]:
        """Get group by ID."""
        result = self.client._execute_query(
            table_name=self.groups_table,
            operation='select',
            filters={'id': group_id}
        )
        # Return the first item since we're querying by unique ID
        return result[0] if result else None
    
    def get_user_groups(self, user_id: int) -> Optional[List[Dict]]:
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
    
    def get_group_members(self, group_id: int) -> Optional[List[Dict]]:
        """Get all members of a group."""
        result = self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='select',
            filters={'group_id': group_id}
        )
        return result if result else []
    
    def add_member_to_group(self, group_id: int, user_id: int) -> Optional[Dict]:
        """Add a user to a group."""
        data = {
            "group_id": group_id,
            "user_id": user_id
        }
        return self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='insert',
            data=data
        )
    
    def remove_member_from_group(self, group_id: int, user_id: int) -> bool:
        """Remove a user from a group."""
        return self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='delete',
            filters={'group_id': group_id, 'user_id': user_id}
        )
    
    def update_group(self, group_id: int, data: Dict[str, Any]) -> Optional[Dict]:
        """Update group information."""
        return self.client._execute_query(
            table_name=self.groups_table,
            operation='update',
            data=data,
            filters={'id': group_id}
        )
    
    def delete_group(self, group_id: int) -> bool:
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
    
    def is_user_member_of_group(self, group_id: int, user_id: int) -> bool:
        """Check if a user is a member of a group."""
        membership = self.client._execute_query(
            table_name=self.group_memberships_table,
            operation='select',
            filters={'group_id': group_id, 'user_id': user_id}
        )
        return bool(membership)
    
    def get_groups_created_by_user(self, user_id: int) -> Optional[List[Dict]]:
        """Get all groups created by a specific user."""
        result = self.client._execute_query(
            table_name=self.groups_table,
            operation='select',
            filters={'created_by': user_id}
        )
        return result if result else [] 