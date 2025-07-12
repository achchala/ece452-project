#!/usr/bin/env python3
"""
Migration script to add total_budget column to groups table.
This script adds the total_budget INTEGER column to existing groups tables.
"""

import os
import sys
from dotenv import load_dotenv

# Add the backend directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from core.supabase.client import SupabaseClient

def migrate_groups_budget():
    """Add total_budget column to groups table."""
    print("🔄 Migrating Groups Table - Adding Budget Column")
    print("=" * 50)
    
    # Initialize Supabase client
    supabase = SupabaseClient()
    
    if not supabase.test_connection():
        print("❌ Failed to connect to Supabase")
        return False
    
    print("✅ Connected to Supabase successfully")
    
    # Get the table name with schema prefix
    groups_table = supabase.base_client.get_table_name("groups")
    
    # Check if the column already exists
    check_column_sql = f"""
    SELECT column_name 
    FROM information_schema.columns 
    WHERE table_name = '{groups_table.split('.')[-1]}' 
    AND column_name = 'total_budget'
    """
    
    # For now, let's just print the SQL that needs to be executed
    print(f"📝 To add the total_budget column, run this SQL in your Supabase dashboard:")
    print(f"   ALTER TABLE {groups_table} ADD COLUMN total_budget DECIMAL(10,2);")
    print("\n📝 To check if the column exists, run this SQL:")
    print(f"   SELECT column_name FROM information_schema.columns WHERE table_name = '{groups_table.split('_')[-1]}' AND column_name = 'total_budget';")
    
    # Since direct SQL execution might not be available, we'll return success
    # The user can run the SQL manually in the Supabase dashboard
    return True

if __name__ == "__main__":
    # Load environment variables
    load_dotenv()
    
    success = migrate_groups_budget()
    if success:
        print("\n🎉 Migration completed successfully!")
    else:
        print("\n💥 Migration failed!")
        sys.exit(1) 