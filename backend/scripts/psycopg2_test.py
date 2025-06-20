import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()

conn = None
try:
    conn = psycopg2.connect(
        dbname=os.getenv("DB_NAME"),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        host=os.getenv("DB_HOST"),
        port=os.getenv("DB_PORT"),
    )
    print("✅ Connection successful!")

    with conn.cursor() as cur:
        cur.execute("SELECT version();")
        print("PostgreSQL version:", cur.fetchone())

        # Example: create a table
        cur.execute("CREATE TABLE IF NOT EXISTS test_table (id serial PRIMARY KEY, name text);")
        conn.commit()
        print("Table created (if not exists).")

except Exception as e:
    print("❌ Connection failed:", e)
finally:
    if conn:
        conn.close() 