import os
import logging
from typing import List, Optional, Dict, Any, Tuple
from supabase import create_client, Client
from dotenv import load_dotenv

logger = logging.getLogger(__name__)

class BaseSupabaseClient:
    """
    Base client for managing Supabase connection using the official Python client.
    """
    def __init__(self):
        # Load environment variables
        load_dotenv()
        
        # Environment configuration
        self.environment = os.getenv("ENVIRONMENT", "development")
        self.table_prefix = f"{self.environment}_" if self.environment != "production" else ""
        
        # Supabase configuration using DB_URL and DB_KEY
        supabase_url = os.getenv("DB_URL")
        supabase_key = os.getenv("DB_KEY")
        
        if not supabase_url or not supabase_key:
            logger.error("DB_URL and DB_KEY must be set in environment variables")
            self.client = None
        else:
            try:
                self.client = create_client(supabase_url, supabase_key)
                logger.info(f"Supabase client created for env: '{self.environment}'")
                logger.info(f"Table prefix: '{self.table_prefix}'")
            except Exception as e:
                logger.error(f"Failed to create Supabase client: {e}")
                self.client = None

    def get_table_name(self, base_table_name: str) -> str:
        """Get the environment-specific table name with prefix."""
        return f"{self.table_prefix}{base_table_name}"
    
    def _execute_query(self, table_name: str, operation: str, data: Optional[Dict] = None, filters: Optional[Dict] = None, limit: Optional[int] = None, select_statement: str = "*") -> Any:
        """
        Execute a query using the Supabase client.
        
        :param table_name: The table to query
        :param operation: The operation to perform ('select', 'insert', 'update', 'delete')
        :param data: Data for insert/update operations
        :param filters: Filters for select/update/delete operations
        :param limit: Limit for select operations
        :param select_statement: The select statement to use for 'select' operations
        :return: Query result
        """
        if not self.client:
            logger.error("Supabase client is not available.")
            return None
            
        try:
            table = self.client.table(table_name)
            
            if operation == 'select':
                query = table.select(select_statement)
                if filters:
                    for key, value in filters.items():
                        query = query.eq(key, value)
                if limit:
                    query = query.limit(limit)
                result = query.execute()
                return result.data
                
            elif operation == 'insert':
                result = table.insert(data).execute()
                return result.data[0] if result.data else None
                
            elif operation == 'update':
                query = table.update(data)
                if filters:
                    for key, value in filters.items():
                        query = query.eq(key, value)
                result = query.execute()
                return result.data[0] if result.data else None
                
            elif operation == 'delete':
                query = table.delete()
                if filters:
                    for key, value in filters.items():
                        query = query.eq(key, value)
                result = query.execute()
                return len(result.data) > 0
                
        except Exception as e:
            logger.error(f"Database query failed: {e}")
            return None

    def test_connection(self) -> bool:
        """Test the database connection."""
        if not self.client:
            return False
            
        try:
            # Try a simple query
            result = self.client.table(self.get_table_name("users")).select("id").limit(1).execute()
            logger.info("✅ Supabase connection successful!")
            return True
        except Exception as e:
            logger.error(f"❌ Supabase connection failed: {e}")
            return False

    def close_connection(self):
        """Close the Supabase client connection."""
        if self.client:
            # The Supabase client handles connection management automatically
            logger.info("Supabase client connection closed.") 