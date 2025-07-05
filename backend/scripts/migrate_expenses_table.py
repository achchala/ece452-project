#!/usr/bin/env python3
"""
Migration script to add group_id column to expenses table.
This script only modifies the expenses table without affecting other tables.
"""

import os
import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

class ExpenseTableMigrator:
    def __init__(self):
        # Database connection parameters
        self.dbname = os.getenv("DB_NAME")
        self.user = os.getenv("DB_USER")
        self.password = os.getenv("DB_PASSWORD")
        self.host = os.getenv("DB_HOST")
        self.port = os.getenv("DB_PORT")
        
        # Environment and table prefix
        self.environment = os.getenv("ENVIRONMENT", "development")
        self.table_prefix = f"{self.environment}_" if self.environment != "production" else ""
        
        print(f"🔧 Environment: {self.environment}")
        print(f"📋 Table prefix: '{self.table_prefix}'")
    
    def get_table_name(self, base_table_name: str) -> str:
        """Get the full table name with prefix."""
        return f"{self.table_prefix}{base_table_name}"
    
    def execute_sql(self, sql):
        """Execute SQL via direct PostgreSQL connection."""
        conn = None
        try:
            conn = psycopg2.connect(
                dbname=self.dbname,
                user=self.user,
                password=self.password,
                host=self.host,
                port=self.port,
                sslmode="require"
            )
            
            with conn.cursor() as cur:
                cur.execute(sql)
                conn.commit()
                print("✅ SQL executed successfully!")
                return True
                
        except Exception as e:
            print(f"❌ SQL execution failed: {e}")
            return False
        finally:
            if conn:
                conn.close()
    
    def column_exists(self, table_name: str, column_name: str) -> bool:
        """Check if a column exists in a table."""
        conn = None
        try:
            conn = psycopg2.connect(
                dbname=self.dbname,
                user=self.user,
                password=self.password,
                host=self.host,
                port=self.port,
                sslmode="require"
            )
            
            with conn.cursor() as cur:
                cur.execute("""
                    SELECT column_name 
                    FROM information_schema.columns 
                    WHERE table_name = %s AND column_name = %s
                """, (table_name, column_name))
                
                result = cur.fetchone()
                return result is not None
        except Exception as e:
            print(f"❌ Error checking column existence: {e}")
            return False
        finally:
            if conn:
                conn.close()
    
    def migrate_expenses_table(self):
        """Add group_id column to expenses table if it doesn't exist."""
        expenses_table = self.get_table_name("expenses")
        groups_table = self.get_table_name("groups")
        
        # Check if group_id column already exists
        if self.column_exists(expenses_table, "group_id"):
            print(f"✅ Column 'group_id' already exists in {expenses_table}")
            return True
        
        # Add group_id column
        sql = f"""
        ALTER TABLE {expenses_table} 
        ADD COLUMN group_id UUID,
        ADD CONSTRAINT fk_expenses_group 
        FOREIGN KEY (group_id) REFERENCES {groups_table}(id) ON DELETE CASCADE;
        """
        
        if self.execute_sql(sql):
            # Create index for the new column
            index_sql = f"CREATE INDEX IF NOT EXISTS {expenses_table}_group_id_idx ON {expenses_table}(group_id);"
            self.execute_sql(index_sql)
            print(f"🎉 Successfully added group_id column to {expenses_table}")
            return True
        else:
            print(f"❌ Failed to add group_id column to {expenses_table}")
            return False

# Run the migration
if __name__ == "__main__":
    migrator = ExpenseTableMigrator()
    migrator.migrate_expenses_table() 