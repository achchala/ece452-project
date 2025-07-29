#!/usr/bin/env python3
"""
Migration script to add due_date column to expenses table.
"""

import os
import sys
from dotenv import load_dotenv

# Add the backend directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from core.supabase.client import SupabaseClient

def add_due_date_column():
    """Add due_date column to expenses table."""
    print("🔄 Adding Due Date Column to Expenses Table")
    print("=" * 50)
    
    # Initialize Supabase client
    supabase = SupabaseClient()
    
    if not supabase.test_connection():
        print("❌ Failed to connect to Supabase")
        return False
    
    print("✅ Connected to Supabase successfully")
    
    # Get the table name with schema prefix
    expenses_table = supabase.base_client.get_table_name("expenses")
    print(f"📋 Target table: {expenses_table}")
    
    # Check if the column already exists
    print("🔍 Checking if due_date column already exists...")
    
    try:
        # Try to select the due_date column - if it doesn't exist, this will fail
        result = supabase.base_client.client.table(expenses_table).select("due_date").limit(1).execute()
        print("✅ due_date column already exists!")
        return True
    except Exception as e:
        if "column" in str(e).lower() and "does not exist" in str(e).lower():
            print("📝 Column does not exist - needs to be added")
        else:
            print(f"⚠️  Unexpected error: {e}")
            return False
    
    print("\n" + "="*50)
    print("📋 MANUAL SQL EXECUTION REQUIRED")
    print("=" * 50)
    print("Please follow these steps:")
    print()
    print("1. Go to your Supabase Dashboard:")
    print("   https://supabase.com/dashboard")
    print()
    print("2. Navigate to your project")
    print()
    print("3. Go to the SQL Editor (in the left sidebar)")
    print()
    print("4. Run this SQL command:")
    print(f"   ALTER TABLE {expenses_table} ADD COLUMN due_date DATE;")
    print()
    print("5. Click 'Run' to execute the command")
    print()
    print("6. Verify the column was added by running:")
    print(f"   SELECT column_name FROM information_schema.columns WHERE table_name = '{expenses_table.split('_')[-1]}' AND column_name = 'due_date';")
    print()
    print("✅ After running the SQL, your expenses table will have the due_date column!")
    
    return True

if __name__ == "__main__":
    # Load environment variables
    load_dotenv()
    
    success = add_due_date_column()
    if success:
        print("\n🎉 Migration helper completed!")
    else:
        print("\n💥 Migration helper failed!")
        sys.exit(1) 