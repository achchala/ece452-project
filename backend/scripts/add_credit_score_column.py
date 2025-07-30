import os
import psycopg2
from datetime import datetime


class CreditScoreColumnMigrator:
    def __init__(self):
        # Database connection parameters
        self.dbname = os.getenv("DB_NAME")
        self.user = os.getenv("DB_USER")
        self.password = os.getenv("DB_PASSWORD")
        self.host = os.getenv("DB_HOST")
        self.port = os.getenv("DB_PORT", "5432")

    def get_table_name(self, base_table_name: str) -> str:
        """Get the full table name with schema prefix."""
        schema = os.getenv("DB_SCHEMA", "public")
        return f"{schema}.{base_table_name}"

    def execute_sql(self, sql):
        """Execute SQL query."""
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
                return True
                
        except Exception as e:
            print(f"❌ SQL execution failed: {e}")
            return False
        finally:
            if conn:
                conn.close()

    def column_exists(self, table_name: str, column_name: str) -> bool:
        """Check if a column exists in a table."""
        sql = f"""
        SELECT EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = '{table_name.split('.')[-1]}'
            AND column_name = '{column_name}'
            AND table_schema = '{table_name.split('.')[0]}'
        );
        """
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
                return cur.fetchone()[0]
                
        except Exception as e:
            print(f"❌ Failed to check column existence: {e}")
            return False
        finally:
            if conn:
                conn.close()

    def add_credit_score_column(self):
        """Add credit_score column to users table if it doesn't exist."""
        users_table = self.get_table_name("users")
        
        # Check if credit_score column already exists
        if self.column_exists(users_table, "credit_score"):
            print(f"✅ Column 'credit_score' already exists in {users_table}")
            return True
        
        # Add credit_score column
        sql = f"""
        ALTER TABLE {users_table} 
        ADD COLUMN credit_score INTEGER;
        """
        
        if self.execute_sql(sql):
            print(f"🎉 Successfully added credit_score column to {users_table}")
            return True
        else:
            print(f"❌ Failed to add credit_score column to {users_table}")
            return False


# Run the migration
if __name__ == "__main__":
    migrator = CreditScoreColumnMigrator()
    if migrator.add_credit_score_column():
        print("\n🎉 Credit score column migration completed successfully!")
    else:
        print("\n❌ Credit score column migration failed!") 