# Backend

This is the Django backend for Evenly

# Backend Setup

## Environment Setup

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

## PostgreSQL Setup

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

## Environment Variables

Create a `.env` file in the backend directory, there is an example .env file in google drive

## Running the Server

1. Start the development server:

```bash
python manage.py runserver
```

2. Test the endpoints:

- Hello World: http://127.0.0.1:8000/
- Database Check: http://127.0.0.1:8000/check-db

The server will start at http://localhost:8000
