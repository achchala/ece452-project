import psycopg2
from psycopg2 import pool
from psycopg2.extras import RealDictCursor
import os
import logging
from typing import List, Optional, Dict, Any, Tuple

logger = logging.getLogger(__name__)

class BaseSupabaseClient:
    """
    Base client for managing a direct PostgreSQL connection pool to Supabase.
    """
    def __init__(self, dbname: str, user: str, password, host: str, port: str, min_conn: int = 1, max_conn: int = 10):
        # Environment configuration
        self.environment = os.getenv("ENVIRONMENT", "development")
        self.table_prefix = f"{self.environment}_" if self.environment != "production" else ""
        
        try:
            self.pool = pool.SimpleConnectionPool(
                min_conn,
                max_conn,
                dbname=dbname,
                user=user,
                password=password,
                host=host,
                port=port,
                sslmode="require"
            )
            logger.info(f"Supabase connection pool created for env: '{self.environment}'")
            logger.info(f"Table prefix: '{self.table_prefix}'")
        except psycopg2.OperationalError as e:
            logger.error(f"Failed to create connection pool: {e}")
            self.pool = None

    def get_table_name(self, base_table_name: str) -> str:
        """Get the environment-specific table name with prefix."""
        return f"{self.table_prefix}{base_table_name}"
    
    def _execute(self, query: str, params: Optional[Tuple] = None, fetch: Optional[str] = None) -> Any:
        """
        Execute a SQL query using a connection from the pool.
        
        :param query: The SQL query to execute.
        :param params: The parameters to pass to the query.
        :param fetch: Type of fetch ('one', 'all'). If None, commits the transaction.
        :return: Fetched data or row count.
        """
        if not self.pool:
            logger.error("Connection pool is not available.")
            return None
            
        conn = None
        try:
            conn = self.pool.getconn()
            # Use RealDictCursor to get results as dictionaries
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(query, params)
                
                if fetch == 'one':
                    result = cur.fetchone()
                    # Commit the transaction for INSERT/UPDATE/DELETE operations
                    conn.commit()
                    return result
                elif fetch == 'all':
                    result = cur.fetchall()
                    # Commit the transaction for INSERT/UPDATE/DELETE operations
                    conn.commit()
                    return result
                else:
                    # For INSERT, UPDATE, DELETE, we commit and can return rowcount
                    conn.commit()
                    return cur.rowcount
        except Exception as e:
            logger.error(f"Database query failed: {e}")
            if conn:
                conn.rollback()
            return None
        finally:
            if conn:
                self.pool.putconn(conn)

    def test_connection(self) -> bool:
        """Test the database connection."""
        if not self.pool:
            return False
            
        res = self._execute("SELECT 1 AS result;", fetch='one')
        if res and res['result'] == 1:
            logger.info("✅ Supabase direct connection successful!")
            return True
        else:
            logger.error("❌ Supabase direct connection failed.")
            return False

    def close_connection(self):
        """Close all connections in the pool."""
        if self.pool:
            self.pool.closeall()
            logger.info("Supabase connection pool closed.") 