"""Main script to reset database and run migrations."""

import os
import sys
from django.db import transaction

# Add the parent directory to Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.reset_db import reset_database
from scripts.run_migrations import run_migrations


def main():
    """Reset database and run migrations."""
    try:
        with transaction.atomic():
            print("Starting database reset process...")

            # Reset database
            if not reset_database():
                raise Exception("Database reset failed")

            # Run migrations
            if not run_migrations():
                raise Exception("Migrations failed")

            print("Database reset and migrations completed successfully!")
            return True

    except Exception as e:
        print(f"Error during database reset and migration: {str(e)}")
        print("Rolling back all changes...")
        return False


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
