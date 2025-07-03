from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class FriendRequestOperations:
    """Handles all friend request-related database operations using the Supabase client."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.table_name = self.client.get_table_name("friend_requests")
    
    def get_by_user(self, user_id: str, status: Optional[str] = None) -> Optional[List[Dict]]:
        """Get all friend requests involving a user."""
        # For this complex query, we'll need to handle it differently
        # since we need OR conditions and status filtering
        if not self.client.client:
            return None
            
        try:
            table = self.client.client.table(self.table_name)
            
            # Get requests where user is either from_user or to_user
            from_requests = table.select("*").eq("from_user", user_id).execute()
            to_requests = table.select("*").eq("to_user", user_id).execute()
            
            all_requests = from_requests.data + to_requests.data
            
            # Filter by status if specified
            if status == 'pending':
                all_requests = [req for req in all_requests if not req.get('request_completed', False)]
            elif status == 'completed':
                all_requests = [req for req in all_requests if req.get('request_completed', False)]
                
            return all_requests
        except Exception as e:
            print(f"Error getting friend requests by user: {e}")
            return None
    
    def get_incoming(self, to_user: str) -> Optional[List[Dict]]:
        """Get incoming pending friend requests for a user."""
        if not self.client.client:
            return None
            
        try:
            result = self.client.client.table(self.table_name).select("*").eq("to_user", to_user).eq("request_completed", False).execute()
            return result.data
        except Exception as e:
            print(f"Error getting incoming friend requests: {e}")
            return None
    
    def get_outgoing(self, from_user: str) -> Optional[List[Dict]]:
        """Get outgoing pending friend requests from a user."""
        if not self.client.client:
            return None
            
        try:
            result = self.client.client.table(self.table_name).select("*").eq("from_user", from_user).eq("request_completed", False).execute()
            return result.data
        except Exception as e:
            print(f"Error getting outgoing friend requests: {e}")
            return None
    
    def create(self, from_user: str, to_user: str) -> Optional[Dict]:
        """Create a friend request."""
        data = {
            "from_user": from_user,
            "to_user": to_user,
            "request_completed": False
        }
        return self.client._execute_query(
            table_name=self.table_name,
            operation='insert',
            data=data
        )
    
    def accept(self, from_user: str, to_user: str) -> bool:
        """Accept a friend request."""
        result = self.client._execute_query(
            table_name=self.table_name,
            operation='update',
            data={'request_completed': True},
            filters={'from_user': from_user, 'to_user': to_user}
        )
        return result is not None
    
    def reject(self, from_user: str, to_user: str) -> bool:
        """Reject a friend request by deleting it."""
        return self.client._execute_query(
            table_name=self.table_name,
            operation='delete',
            filters={'from_user': from_user, 'to_user': to_user}
        )
    
    def get_friends(self, user_email: str) -> Optional[List[Dict]]:
        """Get all friends for a user (where requests are completed)."""
        if not self.client.client:
            return None
        try:
            from_friends = self.client._execute_query(
                table_name=self.table_name,
                operation='select',
                filters={'from_user': user_email, 'request_completed': True}
            )
            to_friends = self.client._execute_query(
                table_name=self.table_name,
                operation='select',
                filters={'to_user': user_email, 'request_completed': True}
            )
            all_friends = (from_friends or []) + (to_friends or [])
            return all_friends
        except Exception as e:
            print(f"Error getting friends: {e}")
            return None 