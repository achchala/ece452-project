import os
import psycopg2
from datetime import datetime


class SplitsPaymentColumnsMigrator:
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

    def migrate_splits_payment_columns(self):
        """Add paid_request and paid_confirmed columns to splits table if they don't exist."""
        splits_table = self.get_table_name("splits")
        
        # Check if paid_request column already exists
        if self.column_exists(splits_table, "paid_request"):
            print(f"✅ Column 'paid_request' already exists in {splits_table}")
        else:
            # Add paid_request column
            sql = f"""
            ALTER TABLE {splits_table} 
            ADD COLUMN paid_request TIMESTAMP WITH TIME ZONE;
            """
            
            if self.execute_sql(sql):
                print(f"🎉 Successfully added paid_request column to {splits_table}")
            else:
                print(f"❌ Failed to add paid_request column to {splits_table}")
                return False

        # Check if paid_confirmed column already exists
        if self.column_exists(splits_table, "paid_confirmed"):
            print(f"✅ Column 'paid_confirmed' already exists in {splits_table}")
        else:
            # Add paid_confirmed column
            sql = f"""
            ALTER TABLE {splits_table} 
            ADD COLUMN paid_confirmed TIMESTAMP WITH TIME ZONE;
            """
            
            if self.execute_sql(sql):
                print(f"🎉 Successfully added paid_confirmed column to {splits_table}")
            else:
                print(f"❌ Failed to add paid_confirmed column to {splits_table}")
                return False

        return True


# Run the migration
if __name__ == "__main__":
    migrator = SplitsPaymentColumnsMigrator()
    if migrator.migrate_splits_payment_columns():
        print("\n🎉 Migration completed successfully!")
    else:
        print("\n❌ Migration failed!") 