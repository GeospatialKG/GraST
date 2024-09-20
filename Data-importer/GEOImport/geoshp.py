import os
import shutil
import uuid
import zipfile
import geopandas as gpd
import pandas as pd
from neo4j import GraphDatabase
from sqlalchemy import create_engine, Integer
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.dialects.postgresql import ARRAY, TEXT
from geoalchemy2 import Geometry, WKTElement
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from typing import Dict
from GEOImport.geohash import cal_geohash
import tempfile
from sqlalchemy.dialects.postgresql import TIMESTAMP

router = APIRouter()
BASE_UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "temp")
Base = declarative_base()


class Neo4jConnection:
    def __init__(self, uri, user, pwd):
        self.__driver = GraphDatabase.driver(uri, auth=(user, pwd))

    def close(self):
        if self.__driver is not None:
            self.__driver.close()

    def write_data(self, data, db_name, nodeClass, isTime, batch_size=5000):
        with self.__driver.session(database=db_name) as session:
            for i in range(0, len(data), batch_size):
                batch_data = data[i:i + batch_size]
                if isTime:
                    session.write_transaction(self._insert_data_with_time, batch_data, nodeClass)
                else:
                    session.write_transaction(self._insert_data_without_time, batch_data, nodeClass)
                print(str(i), 'success')

    @staticmethod
    def _insert_data_with_time(tx, batch_data, nodeClass):
        query = f"""
            UNWIND $data AS row
            MERGE (s:Spatial {{name: '{nodeClass}'}})
            CREATE (n:{nodeClass})
            SET n = row
            WITH s, n, row
            UNWIND row.geohash as gh
            MERGE (g:Geohash {{value: gh}})
            CREATE (n)-[:located]->(g)
            WITH s, n, row
            MERGE (year:Year {{value: row.time.year}})
            MERGE (month:Month {{year: row.time.year, value: row.time.month}})
            MERGE (day:Day {{year: row.time.year, month: row.time.month, value: row.time.day}})
            MERGE (hour:Hour {{year: row.time.year, month: row.time.month, day: row.time.day, value: row.time.hour}})
            MERGE (n)-[:isHourOf]->(hour)
            MERGE (hour)-[:isDayOf]->(day)
            MERGE (day)-[:isMonthOf]->(month)
            MERGE (month)-[:isYearOf]->(year)
            CREATE (s)-[:hasGeoNode]->(n)
        """
        tx.run(query, {'data': batch_data})

    def back(tx, batch_data, nodeClass):
        query = f"""
            UNWIND $data AS row
            MERGE (s:Spatial {{name: '{nodeClass}'}})
            CREATE (n:{nodeClass})
            SET n = row
            WITH s, n, row
            UNWIND row.geohash as gh
            MERGE (g:Geohash {{value: gh}})
            MERGE (n)-[:located]->(g)
            WITH s, n, row
            MERGE (year:Year {{value: row.time.year}})
            MERGE (month:Month {{year: row.time.year, value: row.time.month}})
            MERGE (day:Day {{year: row.time.year, month: row.time.month, value: row.time.day}})
            MERGE (hour:Hour {{year: row.time.year, month: row.time.month, day: row.time.day, value: row.time.hour}})
            MERGE (minute:Minute {{year: row.time.year, month: row.time.month, day: row.time.day, hour: row.time.hour, value: row.time.minute}})
            MERGE (minute)-[:isHourOf]->(hour)
            MERGE (hour)-[:isDayOf]->(day)
            MERGE (day)-[:isMonthOf]->(month)
            MERGE (month)-[:isYearOf]->(year)
            MERGE (n)-[:isMinuteOf]->(minute)
            MERGE (s)-[:hasGeoNode]->(n)
        """

    @staticmethod
    def _insert_data_without_time(tx, batch_data, nodeClass):
        query = f"""
        MERGE (s:Spatial {{name: '{nodeClass}'}})
        WITH s
        UNWIND $data AS row
        CREATE (n:{nodeClass})
        SET n = row
        WITH s, n, row.geohash as geohashes
        UNWIND geohashes as gh
        MERGE (g:Geohash {{value: gh}})
        MERGE (n)-[:located]->(g)
        WITH s, n
        MERGE (s)-[:hasGeoNode]->(n)
        """
        # 确保传递 data 参数
        tx.run(query, {'data': batch_data})


@router.post("/import-shp/")
async def upload_shp(
        file: UploadFile = File(...),
        postgisAddress: str = Form(...),
        postgisUsername: str = Form(...),
        postgisPassword: str = Form(...),
        postgisDatabase: str = Form(...),
        neo4jAddress: str = Form(...),
        neo4jUsername: str = Form(...),
        neo4jPassword: str = Form(...),
        neo4jDatabase: str = Form(...),
        timeColumn: str = Form(None),
        timeColumnSelect: str = Form(None),
        entityType: str = Form(...),
        entityClass: str = Form(...)) -> Dict:

    DATABASE_URL = f'postgresql://{postgisUsername}:{postgisPassword}@{postgisAddress}/{postgisDatabase}'
    engine = create_engine(DATABASE_URL)
    neo4j_conn = None
    upload_dir = os.path.join(BASE_UPLOAD_DIR, str(uuid.uuid4()))
    os.makedirs(upload_dir, exist_ok=True)

    with tempfile.TemporaryDirectory() as upload_dir:  # 使用临时目录
        file_path = os.path.join(upload_dir, file.filename)
        with open(file_path, "wb") as f:
            f.write(await file.read())

        try:
            if zipfile.is_zipfile(file_path):
                with zipfile.ZipFile(file_path, 'r') as zip_ref:
                    zip_ref.extractall(upload_dir)
                    shape_files = [f for f in zip_ref.namelist() if f.endswith('.shp')]
                    if not shape_files:
                        raise HTTPException(status_code=400, detail="No Shapefile found in the zip.")
                    shp_path = os.path.join(upload_dir, shape_files[0])

            gdf = gpd.read_file(shp_path)

            gdf['id'] = range(1, len(gdf) + 1)
            gdf = gdf.set_index('id')

            geomtype = {"Point": "POINT", "Line": "MULTILINESTRING", "Polygon": "MULTIPOLYGON"}.get(entityType)

            gdf['geohash'] = gdf['geometry'].apply(lambda geom: cal_geohash(geom, precision=6))
            gdf['geometry'] = gdf['geometry'].apply(lambda geom: WKTElement(geom.wkt, srid=4326))
            gdf = gdf.rename(columns={'geometry': 'geom'})
            columns_to_write = ['geom', 'geohash']
            dtype = {
                'geom': Geometry(geomtype, srid=4326),
                'geohash': ARRAY(TEXT)
            }

            if timeColumnSelect == 'true' and timeColumn in gdf.columns:
                gdf.rename(columns={timeColumn: 'time'}, inplace=True)
            if timeColumnSelect == 'true' and 'time' in gdf.columns:
                columns_to_write.append('time')
                dtype['time'] = TIMESTAMP(timezone=False)
                gdf['time'] = pd.to_datetime(gdf['time'], unit='s', utc=True)

            gdf[columns_to_write].to_sql(entityClass, engine, if_exists='replace', index=True,
                                         dtype=dtype, method=None)
            gdf_neo = gdf
            gdf_neo['EntityID'] = gdf_neo.index
            data_to_insert = gdf_neo.drop(columns=['geom']).to_dict(orient='records')
            neo4j_conn = Neo4jConnection("bolt://" + neo4jAddress, neo4jUsername, neo4jPassword)
            if timeColumnSelect == 'true':
                neo4j_conn.write_data(data_to_insert, neo4jDatabase, entityClass, True)
            else:
                neo4j_conn.write_data(data_to_insert, neo4jDatabase, entityClass, False)
            return {"message": "Shapefile uploaded and stored successfully."}
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            if neo4j_conn:
                neo4j_conn.close()
            engine.dispose()
