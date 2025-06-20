import os
from dotenv import load_dotenv
from supabase import create_client

load_dotenv()

try:
    # Get Supabase configuration using DB_URL and DB_KEY
    supabase_url = os.getenv("DB_URL")
    supabase_key = os.getenv("DB_KEY")
    
    if not supabase_url or not supabase_key:
        print("❌ DB_URL and DB_KEY must be set in environment variables")
        exit(1)
    
    # Create Supabase client
    supabase = create_client(supabase_url, supabase_key)
    
    # Test connection by trying a simple query
    result = supabase.table("development_users").select("id").limit(1).execute()
    
    print("✅ Connection successful!")
    print("Supabase client connected successfully")
    print("Test query executed successfully")
    
    # Test table creation (if needed)
    # This would be handled by the create_tables.py script
    
except Exception as e:
    print("❌ Connection failed:", e) 