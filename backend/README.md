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

4. Set your IDE's Python interpreter:

```
Open venv -> bin -> copy the absolute path of python.exe
Cmd+Shift+P -> Python: Select Interpreter -> Enter interpreter path -> paste path
```

## Database Setup

This backend connects directly to Supabase using psycopg2. All data is managed via direct SQL queries to the Supabase PostgreSQL database.

### 1. Set Up Environment Variables

- In the `backend` directory, create a `.env` file with the Supabase connection details:
  ```env
  ENVIRONMENT=development
  DB_NAME=postgres
  DB_USER=postgres
  DB_PASSWORD=your_supabase_db_password
  DB_HOST=db.xxxxxxxxxxxxx.supabase.co
  DB_PORT=5432
  ```
- The `ENVIRONMENT` variable controls table prefixing (e.g., `development_users`)

### 2. Verify Connection

- Run the connection test script:
  ```bash
  python scripts/psycopg2_test.py
  ```
  A successful connection prints '✅ Connection successful!' and the PostgreSQL version.

### 3. Create New Tables

When you need to add new features that require new database tables:

1. **Add your table creation logic** to `scripts/create_tables.py`:

   - Follow the existing pattern (see `create_users_table()` and `create_friend_requests_table()`)
   - Use `self.get_table_name("your_table_name")` to get the environment-prefixed table name
   - Include proper indexes and constraints

2. **Test your table creation**:

   ```bash
   python scripts/create_tables.py --create-only
   ```

3. **Considerations for our shared database**:

   - Tables are prefixed by environment (e.g., `development_`, `production_`)
   - Consider backward compatibility when modifying existing tables

4. **Example**: To add a new `products` table, add a method like:
   ```python
   def create_products_table(self):
       table_name = self.get_table_name("products")
       sql = f"""
       CREATE TABLE IF NOT EXISTS {table_name} (
           id BIGSERIAL PRIMARY KEY,
           name VARCHAR(255) NOT NULL,
           price DECIMAL(10,2) NOT NULL,
           created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
       );
       """
       return self.execute_sql(sql)
   ```

### Table Prefixing

- All tables are automatically prefixed by the value of `ENVIRONMENT` (e.g., `development_` for development, no prefix for production)
- Check the correct table when inspecting data in your Supabase dashboard

## Environment Variables

Create a `.env` file in the backend directory with the following structure:

```env
# Environment
ENVIRONMENT=development

# Django Settings
SECRET_KEY=django-insecure-default-key-for-development
ALLOWED_HOSTS=localhost,127.0.0.1,10.0.2.2
DEBUG=True

# Database Settings
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=your_supabase_db_password
DB_HOST=db.xxxxxxxxxxxxx.supabase.co
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
