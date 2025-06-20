from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class FriendRequestOperations:
    """Handles all friend request-related database operations via psycopg2."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.table_name = self.client.get_table_name("friend_requests")
    
    def get_by_user(self, user_id: str, status: Optional[str] = None) -> Optional[List[Dict]]:
        """Get all friend requests involving a user."""
        query = f"SELECT * FROM {self.table_name} WHERE (from_user = %s OR to_user = %s)"
        params = [user_id, user_id]
        
        if status == 'pending':
            query += " AND request_completed = FALSE"
        elif status == 'completed':
            query += " AND request_completed = TRUE"
            
        return self.client._execute(query, tuple(params), fetch='all')
    
    def get_incoming(self, to_user: str) -> Optional[List[Dict]]:
        """Get incoming pending friend requests for a user."""
        query = f"SELECT * FROM {self.table_name} WHERE to_user = %s AND request_completed = FALSE;"
        return self.client._execute(query, (to_user,), fetch='all')
    
    def get_outgoing(self, from_user: str) -> Optional[List[Dict]]:
        """Get outgoing friend requests from a user (both pending and completed)."""
        query = f"SELECT * FROM {self.table_name} WHERE from_user = %s;"
        return self.client._execute(query, (from_user,), fetch='all')
    
    def create(self, from_user: str, to_user: str) -> Optional[Dict]:
        """Create a friend request."""
        query = f"""
            INSERT INTO {self.table_name} (from_user, to_user, request_completed)
            VALUES (%s, %s, FALSE)
            RETURNING *;
        """
        return self.client._execute(query, (from_user, to_user), fetch='one')
    
    def accept(self, from_user: str, to_user: str) -> bool:
        """Accept a friend request."""
        query = f"""
            UPDATE {self.table_name} 
            SET request_completed = TRUE 
            WHERE from_user = %s AND to_user = %s AND request_completed = FALSE;
        """
        rowcount = self.client._execute(query, (from_user, to_user))
        return rowcount > 0
    
    def reject(self, from_user: str, to_user: str) -> bool:
        """Reject/delete a friend request."""
        query = f"DELETE FROM {self.table_name} WHERE from_user = %s AND to_user = %s;"
        rowcount = self.client._execute(query, (from_user, to_user))
        return rowcount > 0
    
    def get_friends(self, user_id: str) -> Optional[List[Dict]]:
        """Get all friends for a user (where requests are completed)."""
        query = f"""
            SELECT * FROM {self.table_name} 
            WHERE (from_user = %s OR to_user = %s) AND request_completed = TRUE;
        """
        return self.client._execute(query, (user_id, user_id), fetch='all') 