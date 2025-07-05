import psycopg2
import os
from datetime import datetime
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

class SupabaseTableCreator:
    def __init__(self):
        # Database connection parameters
        self.dbname = os.getenv("DB_NAME")
        self.user = os.getenv("DB_USER")
        self.password = os.getenv("DB_PASSWORD")
        self.host = os.getenv("DB_HOST")
        self.port = os.getenv("DB_PORT")
        
        # Environment configuration
        self.environment = os.getenv("ENVIRONMENT", "development")
        self.table_prefix = f"{self.environment}_" if self.environment != "production" else ""
        
        print(f"🔧 Environment: {self.environment}")
        print(f"📋 Table prefix: '{self.table_prefix}'")
    
    def get_table_name(self, base_table_name: str) -> str:
        """Get the environment-specific table name with prefix."""
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
    
    def backup_table(self, table_name: str) -> bool:
        """Backup a table by creating a backup table with timestamp."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_table_name = f"{table_name}_backup_{timestamp}"
        
        sql = f"""
        CREATE TABLE IF NOT EXISTS {backup_table_name} AS 
        SELECT * FROM {table_name};
        """
        
        print(f"📦 Creating backup: {table_name} → {backup_table_name}")
        return self.execute_sql(sql)
    
    def delete_table(self, table_name: str) -> bool:
        """Delete a table."""
        sql = f"DROP TABLE IF EXISTS {table_name} CASCADE;"
        
        print(f"🗑️  Deleting table: {table_name}")
        return self.execute_sql(sql)
    
    def backup_and_delete_table(self, table_name: str) -> bool:
        """Backup a table and then delete it."""
        if not self.table_exists(table_name):
            print(f"ℹ️  Table {table_name} doesn't exist, skipping backup/delete")
            return True
        
        if not self.backup_table(table_name):
            return False
        
        if not self.delete_table(table_name):
            return False
        
        print(f"✅ Successfully backed up and deleted {table_name}")
        return True
    
    def table_exists(self, table_name: str) -> bool:
        """Check if a table exists."""
        sql = f"""
        SELECT EXISTS (
            SELECT 1 
            FROM information_schema.tables 
            WHERE table_name = '{table_name}'
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
            print(f"❌ Failed to check table existence: {e}")
            return False
        finally:
            if conn:
                conn.close()
    
    def create_users_table(self):
        """Create users table based on migration 0002_user.py"""
        table_name = self.get_table_name("users")
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            email VARCHAR(254) UNIQUE NOT NULL,
            date_joined TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            firebase_id VARCHAR(255) UNIQUE NOT NULL,
            name VARCHAR(255) NULL
        );
        
        CREATE INDEX IF NOT EXISTS {table_name}_email_idx ON {table_name}(email);
        CREATE INDEX IF NOT EXISTS {table_name}_firebase_id_idx ON {table_name}(firebase_id);
        """
        
        return self.execute_sql(sql)
    
    def create_friend_requests_table(self):
        """Create friend_requests table based on migration 0001_initial.py"""
        table_name = self.get_table_name("friend_requests")
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            from_user VARCHAR(100) NOT NULL,
            to_user VARCHAR(100) NOT NULL,
            request_completed BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            UNIQUE(from_user, to_user)
        );
        
        CREATE INDEX IF NOT EXISTS {table_name}_from_user_idx ON {table_name}(from_user);
        CREATE INDEX IF NOT EXISTS {table_name}_to_user_idx ON {table_name}(to_user);
        """
        
        return self.execute_sql(sql)
    
    def create_groups_table(self):
        """Create groups table"""
        table_name = self.get_table_name("groups")
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name VARCHAR(255) NOT NULL,
            description TEXT,
            created_by UUID NOT NULL,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            FOREIGN KEY (created_by) REFERENCES {self.get_table_name('users')}(id) ON DELETE CASCADE
        );
        
        CREATE INDEX IF NOT EXISTS {table_name}_created_by_idx ON {table_name}(created_by);
        """
        
        return self.execute_sql(sql)
    
    def create_group_memberships_table(self):
        """Create group_memberships table"""
        table_name = self.get_table_name("group_memberships")
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            group_id UUID NOT NULL,
            user_id UUID NOT NULL,
            joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            FOREIGN KEY (group_id) REFERENCES {self.get_table_name('groups')}(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES {self.get_table_name('users')}(id) ON DELETE CASCADE,
            UNIQUE(group_id, user_id)
        );
        
        CREATE INDEX IF NOT EXISTS {table_name}_group_id_idx ON {table_name}(group_id);
        CREATE INDEX IF NOT EXISTS {table_name}_user_id_idx ON {table_name}(user_id);
        """
        
        return self.execute_sql(sql)
    
    def create_expenses_table(self):
        """Create expenses table"""
        table_name = self.get_table_name("expenses")
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            title VARCHAR(255) NOT NULL,
            total_amount INTEGER NOT NULL,
            created_by UUID NOT NULL,
            group_id UUID,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            FOREIGN KEY (created_by) REFERENCES {self.get_table_name('users')}(id) ON DELETE CASCADE,
            FOREIGN KEY (group_id) REFERENCES {self.get_table_name('groups')}(id) ON DELETE CASCADE
        );
        
        CREATE INDEX IF NOT EXISTS {table_name}_created_by_idx ON {table_name}(created_by);
        CREATE INDEX IF NOT EXISTS {table_name}_group_id_idx ON {table_name}(group_id);
        """
        
        return self.execute_sql(sql)
    
    def create_splits_table(self):
        """Create splits table"""
        table_name = self.get_table_name("splits")
        sql = f"""
        CREATE TABLE IF NOT EXISTS {table_name} (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            expenseId UUID NOT NULL,
            userId UUID NOT NULL,
            amount_owed INTEGER NOT NULL,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            FOREIGN KEY (expenseId) REFERENCES {self.get_table_name('expenses')}(id) ON DELETE CASCADE,
            FOREIGN KEY (userId) REFERENCES {self.get_table_name('users')}(id) ON DELETE CASCADE
        );
        
        CREATE INDEX IF NOT EXISTS {table_name}_expense_id_idx ON {table_name}(expenseId);
        CREATE INDEX IF NOT EXISTS {table_name}_user_id_idx ON {table_name}(userId);
        """
        
        return self.execute_sql(sql)
    
    def backup_and_recreate_all_tables(self):
        """Backup existing tables, delete them, and recreate them."""
        users_table = self.get_table_name("users")
        friend_requests_table = self.get_table_name("friend_requests")
        groups_table = self.get_table_name("groups")
        group_memberships_table = self.get_table_name("group_memberships")
        expenses_table = self.get_table_name("expenses")
        splits_table = self.get_table_name("splits")
        
        # Backup and delete in reverse dependency order
        if not self.backup_and_delete_table(splits_table):
            return False
            
        if not self.backup_and_delete_table(expenses_table):
            return False
            
        if not self.backup_and_delete_table(group_memberships_table):
            return False
            
        if not self.backup_and_delete_table(groups_table):
            return False
            
        if not self.backup_and_delete_table(friend_requests_table):
            return False
            
        if not self.backup_and_delete_table(users_table):
            return False
        
        # Create new tables
        if not self.create_users_table():
            return False
        
        if not self.create_friend_requests_table():
            return False
        
        if not self.create_groups_table():
            return False
        
        if not self.create_group_memberships_table():
            return False
        
        if not self.create_expenses_table():
            return False
        
        if not self.create_splits_table():
            return False
        
        print("\n🎉 All tables backed up and recreated successfully!")
        return True
    
    def create_all_tables(self):
        """Create all tables in the correct order (without backup/delete)"""
        if not self.create_users_table():
            return False
        
        if not self.create_friend_requests_table():
            return False
        
        if not self.create_groups_table():
            return False
        
        if not self.create_group_memberships_table():
            return False
        
        if not self.create_expenses_table():
            return False
        
        if not self.create_splits_table():
            return False
        
        print("\n🎉 All tables created successfully!")
        return True

# Run the table creation
if __name__ == "__main__":
    import sys
    
    creator = SupabaseTableCreator()
    
    # Check command line arguments
    if len(sys.argv) > 1 and sys.argv[1] == "--create-only":
        print("🆕 Running in create-only mode (existing tables will not be affected)")
        creator.create_all_tables()
    else:
        print("🔄 Running with backup and recreate mode (default)")
        print("   Use --create-only to skip backup and only create missing tables")
        creator.backup_and_recreate_all_tables() 