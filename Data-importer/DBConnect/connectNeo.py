from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from neo4j import GraphDatabase, basic_auth

router = APIRouter()


class ConnectionParams(BaseModel):
    url: str
    username: str
    password: str


def get_neo4j_session(params: ConnectionParams):
    driver = GraphDatabase.driver(params.url, auth=basic_auth(params.username, params.password))
    return driver.session()


@router.post("/connectNEO/")
def neo4j_connection(params: ConnectionParams):
    session = None
    try:
        params.url = 'bolt://' + params.url
        session = get_neo4j_session(params)
        # Query to fetch the list of databases (could be seen as "projects")
        query = "SHOW DATABASES"
        result = session.run(query)
        databases = []
        for record in result:
            databases.append(record["name"])

        return {
            "message": "Neo4j connection successful",
            "databases": databases
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if session:
            session.close()
