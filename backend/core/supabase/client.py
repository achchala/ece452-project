import os
from dotenv import load_dotenv
import logging
from .base_client import BaseSupabaseClient
from .operations.user_operations import UserOperations
from .operations.friend_request_operations import FriendRequestOperations

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class SupabaseClient:
    """
    Singleton Supabase client with modular operation groups.
    """
    _instance = None
    _initialized = False
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(SupabaseClient, cls).__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not self._initialized:
            # Initialize base client (no parameters needed now)
            self.base_client = BaseSupabaseClient()
            
            # Initialize operation modules
            self.users = UserOperations(self.base_client)
            self.friend_requests = FriendRequestOperations(self.base_client)
            
            self._initialized = True
            logger.info("SupabaseClient initialized with Supabase Python client.")
    
    def test_connection(self) -> bool:
        """Test connection to Supabase."""
        return self.base_client.test_connection()
    
    def close_connection(self):
        """Close the connection."""
        self.base_client.close_connection()

# Global instance
supabase = SupabaseClient() 