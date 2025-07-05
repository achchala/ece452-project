#!/usr/bin/env python3
"""
Test script to verify split creation is working correctly.
"""

import os
import sys
import uuid
from dotenv import load_dotenv

# Add the current directory to the Python path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Load environment variables
load_dotenv()

from core.supabase import supabase

def test_split_creation():
    """Test creating an expense with splits."""
    print("🧪 Testing Split Creation")
    print("=" * 40)
    
    try:
        # Test data with proper UUIDs
        test_title = "Test Expense"
        test_amount = 1000  # $10.00 in cents
        test_firebase_id = str(uuid.uuid4())  # Generate a proper UUID
        test_group_id = str(uuid.uuid4())  # Generate a proper UUID
        test_user_id = str(uuid.uuid4())  # Generate a proper UUID
        
        # Test splits
        test_splits = [
            {"userEmail": "user1@test.com", "amountOwed": 500},
            {"userEmail": "user2@test.com", "amountOwed": 500}
        ]
        
        print(f"📝 Creating expense: {test_title}")
        print(f"💰 Amount: ${test_amount / 100}")
        print(f"👥 Splits: {len(test_splits)} users")
        print(f"🆔 Test Firebase ID: {test_firebase_id}")
        
        # Create the expense
        expense = supabase.expenses.create_expense(
            title=test_title,
            total_amount=test_amount,
            created_by=test_firebase_id,
            group_id=test_group_id
        )
        
        if expense:
            print(f"✅ Expense created with ID: {expense.get('id')}")
            
            # Create splits
            expense_id = expense.get('id')
            created_splits = []
            
            for split in test_splits:
                user_email = split.get("userEmail")
                amount_owed = split.get("amountOwed")
                
                print(f"🔗 Creating split for {user_email}: ${amount_owed / 100}")
                
                # Create the split with the test user ID
                split_data = supabase.expenses.create_split(
                    expense_id, 
                    test_user_id,  # Use the generated UUID
                    amount_owed
                )
                
                if split_data:
                    print(f"✅ Split created: {split_data}")
                    created_splits.append(split_data)
                else:
                    print(f"❌ Failed to create split for {user_email}")
            
            print(f"\n📊 Summary:")
            print(f"   - Expense created: ✅")
            print(f"   - Splits created: {len(created_splits)}/{len(test_splits)}")
            
            # Test retrieving the expense with splits
            expense_with_splits = supabase.expenses.get_expense_with_splits(expense_id)
            if expense_with_splits:
                splits_count = len(expense_with_splits.get('splits', []))
                print(f"   - Splits retrieved: {splits_count}")
                
                # Show the splits data
                if splits_count > 0:
                    print(f"   - Split details:")
                    for split in expense_with_splits.get('splits', []):
                        print(f"     * User ID: {split.get('userid')}, Amount: ${split.get('amount_owed', 0) / 100}")
            else:
                print(f"   - Failed to retrieve expense with splits")
                
        else:
            print("❌ Failed to create expense")
            
    except Exception as e:
        print(f"❌ Error during test: {e}")
        import traceback
        traceback.print_exc()

def test_database_connection():
    """Test database connection."""
    print("🔌 Testing Database Connection")
    print("=" * 40)
    
    if supabase.test_connection():
        print("✅ Database connection successful")
        return True
    else:
        print("❌ Database connection failed")
        return False

if __name__ == "__main__":
    print("🚀 Starting Split Creation Test")
    print("=" * 50)
    
    # Test database connection first
    if test_database_connection():
        test_split_creation()
    else:
        print("❌ Cannot proceed without database connection") 