"""Script to run migrations."""

import os
import sys
import django
from django.db import transaction
from django.core.management import call_command

# Add the parent directory to Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Set up Django
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "core.settings")
django.setup()


def run_migrations():
    """Run all migrations."""
    try:
        with transaction.atomic():
            print("Running migrations...")

            # Only run migrate, don't create new migrations
            call_command("migrate")

            print("Migrations completed successfully!")
            return True
    except Exception as e:
        print(f"Error running migrations: {str(e)}")
        return False


if __name__ == "__main__":
    run_migrations()
