from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class UserOperations:
    """Handles all user-related database operations via psycopg2."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.table_name = self.client.get_table_name("users")
    
    def get_by_email(self, email: str) -> Optional[Dict]:
        """Get user by email."""
        query = f"SELECT * FROM {self.table_name} WHERE email = %s;"
        return self.client._execute(query, (email,), fetch='one')
    
    def get_by_firebase_id(self, firebase_id: str) -> Optional[Dict]:
        """Get user by Firebase ID."""
        query = f"SELECT * FROM {self.table_name} WHERE firebase_id = %s;"
        return self.client._execute(query, (firebase_id,), fetch='one')
    
    def get_by_id(self, user_id: int) -> Optional[Dict]:
        """Get user by ID."""
        query = f"SELECT * FROM {self.table_name} WHERE id = %s;"
        return self.client._execute(query, (user_id,), fetch='one')
    
    def create(self, email: str, firebase_id: str) -> Optional[Dict]:
        """Create a new user and return the created record."""
        query = f"""
            INSERT INTO {self.table_name} (email, firebase_id) 
            VALUES (%s, %s) 
            RETURNING *;
        """
        return self.client._execute(query, (email, firebase_id), fetch='one')
    
    def update(self, user_id: int, data: Dict[str, Any]) -> Optional[Dict]:
        """Update user data and return the updated record."""
        set_clause = ", ".join([f"{key} = %s" for key in data.keys()])
        query = f"UPDATE {self.table_name} SET {set_clause} WHERE id = %s RETURNING *;"
        params = list(data.values()) + [user_id]
        return self.client._execute(query, tuple(params), fetch='one')
    
    def delete(self, user_id: int) -> bool:
        """Delete a user. Returns True if a row was deleted."""
        query = f"DELETE FROM {self.table_name} WHERE id = %s;"
        rowcount = self.client._execute(query, (user_id,))
        return rowcount > 0
    
    def list_all(self, limit: Optional[int] = None) -> Optional[List[Dict]]:
        """List all users."""
        query = f"SELECT * FROM {self.table_name}"
        params = None
        if limit:
            query += " LIMIT %s"
            params = (limit,)
        return self.client._execute(query, params, fetch='all') 