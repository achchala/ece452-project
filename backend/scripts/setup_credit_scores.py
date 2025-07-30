#!/usr/bin/env python3
"""
Script to calculate and populate credit scores for all users.
This script only updates the credit_score column values, no schema changes.
"""

import os
import sys
from datetime import datetime

# Add the backend directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from core.supabase import supabase
from core.supabase.operations.credit_score_operations import CreditScoreOperations


def main():
    """Calculate and populate credit scores for all users."""
    print("🚀 Starting Credit Score Population Process...")
    print("=" * 50)
    
    # Check if credit_score column exists
    print("\n📋 Step 1: Checking database schema...")
    try:
        # Try to select from users table with credit_score column
        test_query = supabase.base_client._execute_query(
            table_name=supabase.base_client.get_table_name("users"),
            operation='select',
            filters={'credit_score': None},
            limit=1
        )
        print("✅ Credit score column exists in users table")
    except Exception as e:
        print(f"❌ Credit score column not found: {e}")
        print("Please ensure the credit_score column has been added to the users table.")
        return False
    
    # Calculate initial credit scores
    print("\n📋 Step 2: Calculating credit scores for all users...")
    credit_score_ops = CreditScoreOperations(supabase.base_client)
    
    try:
        results = credit_score_ops.update_all_credit_scores()
        
        print(f"✅ Credit score calculation completed!")
        print(f"📊 Results:")
        print(f"   - Total users: {results['total_users']}")
        print(f"   - Updated users: {results['updated_users']}")
        print(f"   - Failed users: {results['failed_users']}")
        print(f"   - Users with scores: {results['users_with_scores']}")
        print(f"   - Users without history: {results['users_without_history']}")
        
        # Show some example credit scores
        print(f"\n📋 Step 3: Sample credit scores...")
        users = supabase.base_client._execute_query(
            table_name=supabase.base_client.get_table_name("users"),
            operation='select',
            filters={'credit_score__not': None}
        )
        
        if users:
            print("Users with credit scores:")
            for user in users[:5]:  # Show first 5 users
                name = user.get('name', 'Unknown')
                email = user.get('email', 'No email')
                score = user.get('credit_score')
                print(f"   - {name} ({email}): {score}")
        else:
            print("No users have credit scores yet (no payment history)")
        
        # Show users without scores
        users_without_scores = supabase.base_client._execute_query(
            table_name=supabase.base_client.get_table_name("users"),
            operation='select',
            filters={'credit_score': None}
        )
        
        if users_without_scores:
            print(f"\nUsers without credit scores (no payment history):")
            for user in users_without_scores[:5]:  # Show first 5 users
                name = user.get('name', 'Unknown')
                email = user.get('email', 'No email')
                print(f"   - {name} ({email}): NULL")
        
        return True
        
    except Exception as e:
        print(f"❌ Credit score calculation failed: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    success = main()
    if success:
        print("\n🎉 Credit Score Population Completed Successfully!")
        print("=" * 50)
        print("✅ Credit scores calculated and populated")
        print("✅ System ready for credit score functionality")
    else:
        print("\n❌ Credit Score Population Failed!")
        print("=" * 50)
        print("Please check the error messages above and try again.")
        sys.exit(1) 