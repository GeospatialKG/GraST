from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import psycopg2
from psycopg2 import OperationalError

router = APIRouter()

class ConnectionParams(BaseModel):
    url: str  # This is the base URL, e.g., "postgresql://localhost:5432"
    username: str
    password: str

    def construct_full_url(self):
        # Construct the full connection URL with username and password
        return f"{self.url}?user={self.username}&password={self.password}"

def get_postgresql_connection(params: ConnectionParams):
    try:
        # Establish a connection using the constructed PostgreSQL URL
        connection = psycopg2.connect(params.construct_full_url())
        return connection
    except OperationalError as e:
        raise HTTPException(status_code=500, detail="Unexpected error")

@router.post("/connectPG/")
def postgresql_connection(params: ConnectionParams):
    params.url = 'postgresql://' + params.url
    connection = None
    try:
        connection = get_postgresql_connection(params)
        # Query to fetch all database names
        with connection.cursor() as cursor:
            cursor.execute("SELECT datname FROM pg_database WHERE datistemplate = false;")
            databases = cursor.fetchall()
            database_list = [db[0] for db in databases]

        return {
            "message": "PostgreSQL connection successful",
            "databases": database_list
        }
    except HTTPException as e:
        raise e  # Re-raise the HTTP exception with a formatted error message
    except Exception as e:
        raise HTTPException(status_code=500)
    finally:
        if connection:
            connection.close()
