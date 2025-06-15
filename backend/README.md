# Backend

This is the Django backend for Evenly

## Server Setup

1. Create a virtual environment:

```bash
python -m venv venv
```

2. Activate the virtual environment:

- Windows:

```bash
.\venv\Scripts\activate
```

- Unix/MacOS:

```bash
source venv/bin/activate
```

3. Install dependencies:

```bash
pip install -r requirements.txt
```

## Database Setup

### PostgreSQL Setup

1. Install PostgreSQL:

   - Download and install from [PostgreSQL website](https://www.postgresql.org/download/)
   - During installation, note down the password you set for the postgres user

2. Create Database:

   - Open pgAdmin (comes with PostgreSQL)
   - Connect to your server
   - Right-click on Databases → Create → Database
   - Name it whatever you like, but make sure it aligns with your env variables

3. Verify Connection:
   - Test the connection using the `/check-db` endpoint
   - If you get connection errors, verify:
     - PostgreSQL service is running
     - Database credentials in `.env` match your setup
     - Port 5432 is not blocked by firewall

### Initializing Database Models

To initialize or reset the database models, use the provided scripts in the `scripts` directory:

```bash
# Reset database and run migrations
python scripts/reset_and_migrate.py
```

This script will:

1. Drop all existing tables in the database
2. Clear the migrations history
3. Apply all existing migrations to create fresh tables

## Environment Variables

Create a `.env` file in the backend directory with the following structure:

```env
# Environment
ENVIRONMENT=development

# Django Settings
SECRET_KEY=django-insecure-default-key-for-development
ALLOWED_HOSTS=localhost,127.0.0.1
DEBUG=True

# Database Settings
DB_NAME=your_database_name
DB_USER=postgres
DB_PASSWORD=your_postgres_password
DB_HOST=localhost
DB_PORT=5432
```

Note: Replace the placeholder values with your actual configuration.

## Running the Server

Start the development server:

```bash
python main.py runserver
```

Test Endpoints

- Hello World: http://localhost:8000/
- Database Check: http://localhost:8000/check-db
