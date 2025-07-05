#!/usr/bin/env python3
"""
Test script to verify group membership functionality.
This script tests adding members to groups and retrieving group members with user data.
"""

import os
import sys
from dotenv import load_dotenv

# Add the backend directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), 'core'))

from core.supabase.client import SupabaseClient

def test_group_membership():
    """Test group membership functionality."""
    print("🧪 Testing Group Membership Functionality")
    print("=" * 50)
    
    # Initialize Supabase client
    supabase = SupabaseClient()
    
    if not supabase.test_connection():
        print("❌ Failed to connect to Supabase")
        return False
    
    print("✅ Connected to Supabase successfully")
    
    # Test 1: Create a test user
    print("\n📝 Test 1: Creating test user...")
    import time
    timestamp = int(time.time())
    test_user_data = {
        "email": f"test{timestamp}@example.com",
        "firebase_id": f"test_firebase_id_{timestamp}",
        "name": "Test User"
    }
    
    user = supabase.users.create(
        test_user_data["email"],
        test_user_data["firebase_id"]
    )
    
    if not user:
        print("❌ Failed to create test user")
        return False
    
    print(f"✅ Created test user: {user.get('name')} ({user.get('email')})")
    user_id = user.get('id')
    print(f"   User ID: {user_id}")
    
    # Test 2: Create a test group
    print("\n📝 Test 2: Creating test group...")
    group = supabase.groups.create_group(
        "Test Group",
        "A test group for membership testing",
        user_id
    )
    
    if not group:
        print("❌ Failed to create test group")
        return False
    
    print(f"✅ Created test group: {group.get('name')}")
    group_id = group.get('id')
    print(f"   Group ID: {group_id}")
    
    # Test 3: Create another test user to add as member
    print("\n📝 Test 3: Creating second test user...")
    member_user_data = {
        "email": f"member{timestamp}@example.com",
        "firebase_id": f"test_firebase_id_{timestamp + 1}",
        "name": "Member User"
    }
    
    member_user = supabase.users.create(
        member_user_data["email"],
        member_user_data["firebase_id"]
    )
    
    if not member_user:
        print("❌ Failed to create member user")
        return False
    
    print(f"✅ Created member user: {member_user.get('name')} ({member_user.get('email')})")
    member_user_id = member_user.get('id')
    print(f"   Member User ID: {member_user_id}")
    
    # Test 4: Add member to group
    print("\n📝 Test 4: Adding member to group...")
    membership = supabase.groups.add_member_to_group(group_id, member_user_id)
    
    if not membership:
        print("❌ Failed to add member to group")
        return False
    
    print(f"✅ Added member to group. Membership ID: {membership.get('id')}")
    print(f"   Joined at: {membership.get('joined_at')}")
    
    # Test 5: Get group members with user data
    print("\n📝 Test 5: Retrieving group members with user data...")
    members = supabase.groups.get_group_members(group_id)
    
    if not members:
        print("❌ Failed to retrieve group members")
        return False
    
    print(f"✅ Retrieved {len(members)} group members:")
    for member in members:
        print(f"   - Member ID: {member.get('id')}")
        print(f"     User ID: {member.get('user_id')}")
        print(f"     Joined at: {member.get('joined_at')}")
        if member.get('user'):
            user_data = member.get('user')
            print(f"     User name: {user_data.get('name')}")
            print(f"     User email: {user_data.get('email')}")
        else:
            print(f"     User data: None")
        print()
    
    # Test 6: Get group detail with members
    print("\n📝 Test 6: Retrieving group detail with members...")
    group_detail = supabase.groups.get_group_by_id(group_id)
    
    if not group_detail:
        print("❌ Failed to retrieve group detail")
        return False
    
    print(f"✅ Retrieved group detail: {group_detail.get('name')}")
    
    # Test 7: Clean up test data
    print("\n📝 Test 7: Cleaning up test data...")
    
    # Delete the group (this should cascade delete memberships)
    success = supabase.groups.delete_group(group_id)
    if success:
        print("✅ Deleted test group")
    else:
        print("❌ Failed to delete test group")
    
    # Delete test users
    # Note: In a real application, you might want to implement user deletion
    # For now, we'll just note that cleanup is needed
    print("ℹ️  Test users created (cleanup needed manually):")
    print(f"   - {test_user_data['email']} (ID: {user_id})")
    print(f"   - {member_user_data['email']} (ID: {member_user_id})")
    
    print("\n🎉 All tests completed successfully!")
    return True

if __name__ == "__main__":
    success = test_group_membership()
    sys.exit(0 if success else 1) 