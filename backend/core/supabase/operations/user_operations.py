from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class UserOperations:
    """Handles all user-related database operations using the Supabase client."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.table_name = self.client.get_table_name("users")
    
    def get_by_email(self, email: str) -> Optional[Dict]:
        """Get user by email."""
        result = self.client._execute_query(
            table_name=self.table_name,
            operation='select',
            filters={'email': email}
        )
        return result[0] if result else None
    
    def get_by_firebase_id(self, firebase_id: str) -> Optional[Dict]:
        """Get user by Firebase ID."""
        result = self.client._execute_query(
            table_name=self.table_name,
            operation='select',
            filters={'firebase_id': firebase_id}
        )
        return result[0] if result else None
    
    def get_by_id(self, user_id: str) -> Optional[Dict]:
        """Get user by ID."""
        result = self.client._execute_query(
            table_name=self.table_name,
            operation='select',
            filters={'id': user_id}
        )
        return result[0] if result else None
    
    def create(self, email: str, firebase_id: str) -> Optional[Dict]:
        """Create a new user and return the created record."""
        data = {
            "email": email,
            "firebase_id": firebase_id
        }
        return self.client._execute_query(
            table_name=self.table_name,
            operation='insert',
            data=data
        )
    
    def update_name(self, firebase_id: str, name: str) -> Optional[Dict]:
        """Update user's name and return the updated record."""
        return self.client._execute_query(
            table_name=self.table_name,
            operation='update',
            data={'name': name},
            filters={'firebase_id': firebase_id}
        )
    
    def update(self, user_id: str, data: Dict[str, Any]) -> Optional[Dict]:
        """Update user data and return the updated record."""
        return self.client._execute_query(
            table_name=self.table_name,
            operation='update',
            data=data,
            filters={'id': user_id}
        )
    
    def delete(self, user_id: str) -> bool:
        """Delete a user. Returns True if a row was deleted."""
        return self.client._execute_query(
            table_name=self.table_name,
            operation='delete',
            filters={'id': user_id}
        )
    
    def list_all(self, limit: Optional[int] = None) -> Optional[List[Dict]]:
        """List all users."""
        return self.client._execute_query(
            table_name=self.table_name,
            operation='select',
            limit=limit
        ) 