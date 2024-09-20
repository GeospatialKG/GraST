from fastapi import APIRouter, UploadFile, File, Form, HTTPException
import os
import psycopg2
import pygeohash as pgh
from osgeo import gdal
import tempfile
import shutil
from GEOImport.rastWKB import wkblify_raster
from GEOImport.geoshp import Neo4jConnection

router = APIRouter()


class Options:
    def __init__(self, version, endian, srid):
        self.version = version
        self.endian = endian
        self.srid = srid
        self.band = None
        self.overview_level = 1
        self.block_size = None
        self.create_table = 0
        self.register = 0
        self.column = 'test'
        self.table = 'test'
        self.filename = 'test'
        self.output = 'test'


def geohash_to_bounds(gh):
    decoded = pgh.decode_exactly(gh)
    latitude, longitude = decoded[0], decoded[1]
    lat_err, lon_err = decoded[2], decoded[3]
    return (latitude - lat_err, longitude - lon_err, latitude + lat_err, longitude + lon_err)


def get_pixel_coordinates(geo_transform, lat, lon, max_x, max_y):
    x = int((lon - geo_transform[0]) / geo_transform[1])
    y = max_y - int((lat - geo_transform[3]) / geo_transform[5])  # Reverse Y coordinate
    return max(0, min(x, max_x)), max(0, min(y, max_y))


def generate_geohashes(min_lat, min_lon, max_lat, max_lon, precision):
    geohashes = set()
    lat_step = (max_lat - min_lat) / 100
    lon_step = (max_lon - min_lon) / 100
    lat = min_lat
    while lat <= max_lat:
        lon = min_lon
        while lon <= max_lon:
            geohashes.add(pgh.encode(lat, lon, precision))
            lon += lon_step
        lat += lat_step
    return geohashes


def rastGeoHash(postgisAddress, postgisUsername, postgisPassword, postgisDatabase, neo4jAddress, neo4jUsername, neo4jPassword, neo4jDatabase, entityClass, input_file, precision=6):
    output_folder = tempfile.mkdtemp()
    try:
        neo4j_conn = None
        dataset = gdal.Open(input_file)
        geo_transform = dataset.GetGeoTransform()
        max_x = dataset.RasterXSize
        max_y = dataset.RasterYSize
        min_lon = geo_transform[0]
        max_lat = geo_transform[3]
        max_lon = min_lon + geo_transform[1] * max_x
        min_lat = max_lat + geo_transform[5] * max_y

        geohashes = generate_geohashes(min_lat, min_lon, max_lat, max_lon, precision)

        # 设置数据库连接
        pg_conn = psycopg2.connect(host=postgisAddress.split(':')[0], dbname=postgisDatabase, user=postgisUsername, password=postgisPassword, port=postgisAddress.split(':')[1])
        cur = pg_conn.cursor()
        neo4j_conn = Neo4jConnection("bolt://" + neo4jAddress, neo4jUsername, neo4jPassword)

        # 检查并创建 raster_table
        cur.execute(f'DROP TABLE IF EXISTS "{entityClass}";')
        cur.execute(f"""
        CREATE TABLE "{entityClass}" (
            rid SERIAL PRIMARY KEY,
            geohash VARCHAR(18),
            rast RASTER
        );
        """)

        for gh in geohashes:
            bounds = geohash_to_bounds(gh)
            x_min, y_max = get_pixel_coordinates(geo_transform, bounds[2], bounds[1], max_x, max_y)
            x_max, y_min = get_pixel_coordinates(geo_transform, bounds[0], bounds[3], max_x, max_y)
            width = x_max - x_min
            height = y_max - y_min
            if width <= 0 or height <= 0:
                continue
            output_file = os.path.join(output_folder, f"{gh}.tif")
            gdal.Translate(output_file, input_file, srcWin=[x_min, y_min, width, height], format="GTiff")
            options = Options(version=0, endian=1, srid=4326)
            raster_wkb = wkblify_raster(options, output_file, 0)
            wkb_str = str(raster_wkb[2]).replace("b'", "").replace("'", "")

            query = f'INSERT INTO "{entityClass}" (geohash, rast) VALUES (%s, ST_RastFromHexWKB(%s)) RETURNING rid'
            cur.execute(query, (gh, wkb_str))
            rid = cur.fetchone()[0]  # Fetches the first column of the first row
            pg_conn.commit()

            geo_data = [{"rid": rid,"geohash": gh}]

            neo4j_conn.write_data(geo_data, neo4jDatabase, entityClass, False)

            print(f"Inserted GeoHash {gh} into database.")
    finally:
        dataset = None
        neo4j_conn.close()
        pg_conn.close()
        shutil.rmtree(output_folder)

@router.post("/import-tif/")
async def upload_tiff(
        file: UploadFile = File(...),
        postgisAddress: str = Form(...),
        postgisUsername: str = Form(...),
        postgisPassword: str = Form(...),
        postgisDatabase: str = Form(...),
        neo4jAddress: str = Form(...),
        neo4jUsername: str = Form(...),
        neo4jPassword: str = Form(...),
        neo4jDatabase: str = Form(...),
        entityClass: str = Form(...),
        precision: int = 6,
):
    if file.content_type != 'image/tiff':
        raise HTTPException(status_code=400, detail="Unsupported file type. Please upload a TIFF file.")
    temp_dir = tempfile.mkdtemp()
    try:
        temp_file_path = os.path.join(temp_dir, file.filename)
        with open(temp_file_path, 'wb') as f:
            f.write(await file.read())
        rastGeoHash(postgisAddress, postgisUsername, postgisPassword, postgisDatabase, neo4jAddress, neo4jUsername, neo4jPassword, neo4jDatabase, entityClass, temp_file_path, precision)
        return {"message": "Tiff uploaded and stored successfully."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        os.remove(temp_file_path)
        shutil.rmtree(temp_dir)
