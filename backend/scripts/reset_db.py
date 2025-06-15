"""Script to reset the database."""

import os
import sys
import django
from django.db import connection, transaction

# Add the parent directory to Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Set up Django
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "core.settings")
django.setup()


def reset_database():
    """Reset the database by dropping all tables and running migrations."""
    try:
        with transaction.atomic():
            with connection.cursor() as cursor:
                # First, clear the migrations table
                cursor.execute("TRUNCATE TABLE django_migrations CASCADE")
                print("Cleared migrations history")

                # Get all table names
                cursor.execute(
                    """
                    SELECT table_name 
                    FROM information_schema.tables 
                    WHERE table_schema = 'public'
                """
                )
                tables = cursor.fetchall()

                # Drop all tables
                for table in tables:
                    table_name = table[0]
                    # Drop both possible table names for friend_requests
                    if table_name in ["friend_requests", "core_friend_requests"]:
                        cursor.execute('DROP TABLE IF EXISTS "friend_requests" CASCADE')
                        cursor.execute(
                            'DROP TABLE IF EXISTS "core_friend_requests" CASCADE'
                        )
                    else:
                        cursor.execute(f'DROP TABLE IF EXISTS "{table_name}" CASCADE')

                print("All tables dropped successfully!")
                return True
    except Exception as e:
        print(f"Error resetting database: {str(e)}")
        return False


if __name__ == "__main__":
    reset_database()
