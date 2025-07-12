#!/usr/bin/env python3
"""
Simple script to add total_budget column to groups table.
This script will guide you through the process and provide the exact SQL to run.
"""

import os
import sys
from dotenv import load_dotenv

# Add the backend directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from core.supabase.client import SupabaseClient

def check_and_add_budget_column():
    """Check if budget column exists and provide instructions to add it."""
    print("🔄 Budget Column Migration Helper")
    print("=" * 50)
    
    # Initialize Supabase client
    supabase = SupabaseClient()
    
    if not supabase.test_connection():
        print("❌ Failed to connect to Supabase")
        return False
    
    print("✅ Connected to Supabase successfully")
    
    # Get the table name with schema prefix
    groups_table = supabase.base_client.get_table_name("development_groups")
    print(f"📋 Target table: {groups_table}")
    
    # Check if the column already exists
    print("🔍 Checking if total_budget column already exists...")
    
    try:
        # Try to select the total_budget column - if it doesn't exist, this will fail
        result = supabase.base_client.client.table(groups_table).select("total_budget").limit(1).execute()
        print("✅ total_budget column already exists!")
        print("🎉 No action needed - your database is ready!")
        return True
    except Exception as e:
        if "column" in str(e).lower() and "does not exist" in str(e).lower():
            print("📝 Column does not exist - needs to be added")
        else:
            print(f"⚠️  Unexpected error: {e}")
            return False
    
    print("\n" + "="*50)
    print("📋 MANUAL SQL EXECUTION REQUIRED")
    print("="*50)
    print("Since direct SQL execution is not available, please follow these steps:")
    print()
    print("1. Go to your Supabase Dashboard:")
    print("   https://supabase.com/dashboard")
    print()
    print("2. Navigate to your project")
    print()
    print("3. Go to the SQL Editor (in the left sidebar)")
    print()
    print("4. Run this SQL command:")
    print(f"   ALTER TABLE {groups_table} ADD COLUMN total_budget DECIMAL(10,2);")
    print()
    print("5. Click 'Run' to execute the command")
    print()
    print("6. Verify the column was added by running:")
    print(f"   SELECT column_name FROM information_schema.columns WHERE table_name = '{groups_table.split('_')[-1]}' AND column_name = 'total_budget';")
    print()
    print("✅ After running the SQL, your groups table will have the budget column!")
    print("✅ All existing data will be preserved - new groups can have budgets!")
    
    return True

if __name__ == "__main__":
    # Load environment variables
    load_dotenv()
    
    success = check_and_add_budget_column()
    if success:
        print("\n🎉 Migration helper completed!")
    else:
        print("\n💥 Migration helper failed!")
        sys.exit(1) 