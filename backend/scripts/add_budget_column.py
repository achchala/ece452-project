#!/usr/bin/env python3
"""
Script to add total_budget column to groups table using direct SQL execution.
"""

import os
import sys
from dotenv import load_dotenv

# Add the backend directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from core.supabase.client import SupabaseClient

def add_budget_column():
    """Add total_budget column to groups table."""
    print("🔄 Adding Budget Column to Groups Table")
    print("=" * 50)
    
    # Initialize Supabase client
    supabase = SupabaseClient()
    
    if not supabase.test_connection():
        print("❌ Failed to connect to Supabase")
        return False
    
    print("✅ Connected to Supabase successfully")
    
    # Get the table name with schema prefix
    groups_table = supabase.base_client.get_table_name("groups")
    print(f"📋 Target table: {groups_table}")
    
    try:
        # First, check if the column already exists
        print("🔍 Checking if total_budget column already exists...")
        
        # Use a simple select query to check if the column exists
        try:
            # Try to select the total_budget column - if it doesn't exist, this will fail
            result = supabase.base_client.client.table(groups_table).select("total_budget").limit(1).execute()
            print("✅ total_budget column already exists!")
            return True
        except Exception as e:
            if "column" in str(e).lower() and "does not exist" in str(e).lower():
                print("📝 Column does not exist, proceeding to add it...")
            else:
                print(f"⚠️  Unexpected error checking column: {e}")
                return False
        
        # Since we can't execute ALTER TABLE directly through the REST API,
        # we'll use the PostgreSQL function approach
        print("🔧 Adding total_budget column...")
        
        # Create a function to add the column if it doesn't exist
        create_function_sql = """
        CREATE OR REPLACE FUNCTION add_budget_column_if_not_exists()
        RETURNS void AS $$
        BEGIN
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.columns 
                WHERE table_name = 'development_groups' 
                AND column_name = 'total_budget'
            ) THEN
                ALTER TABLE development_groups ADD COLUMN total_budget DECIMAL(10,2);
                RAISE NOTICE 'total_budget column added successfully';
            ELSE
                RAISE NOTICE 'total_budget column already exists';
            END IF;
        END;
        $$ LANGUAGE plpgsql;
        """
        
        # Execute the function creation
        try:
            supabase.base_client.client.rpc('exec_sql', {'sql': create_function_sql}).execute()
            print("✅ Function created successfully")
        except Exception as e:
            print(f"⚠️  Could not create function: {e}")
            print("📝 Please run this SQL manually in your Supabase dashboard:")
            print(f"   ALTER TABLE {groups_table} ADD COLUMN total_budget DECIMAL(10,2);")
            return False
        
        # Execute the function to add the column
        try:
            supabase.base_client.client.rpc('add_budget_column_if_not_exists').execute()
            print("✅ total_budget column added successfully!")
            return True
        except Exception as e:
            print(f"❌ Error executing function: {e}")
            print("📝 Please run this SQL manually in your Supabase dashboard:")
            print(f"   ALTER TABLE {groups_table} ADD COLUMN total_budget DECIMAL(10,2);")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

if __name__ == "__main__":
    # Load environment variables
    load_dotenv()
    
    success = add_budget_column()
    if success:
        print("\n🎉 Budget column migration completed successfully!")
    else:
        print("\n💥 Migration failed! Please run the SQL manually.")
        sys.exit(1) 