
# GraST: Geospatial-Temporal Semantic Query Optimization Framework



[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Neo4j Version](https://img.shields.io/badge/Neo4j-5.x-green.svg)](https://neo4j.com/download-center/)
[![PostgreSQL Version](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/download/)

## Introduction

GraST is a high-performance framework for managing and querying geospatial-temporal data in graph databases. It achieves efficient querying by structurally storing geographic entities in both graph databases and relational databases, and performing data mapping and SQL conversion.

GraST currently supports Neo4j 5.x and PostgreSQL 16 (with PostGIS extension). Please ensure these databases are installed before using GraST.

## Features & Advantages

- 🌉 Hybrid storage leveraging both graph and relational databases
- 🗺️ Efficient spatiotemporal indexing (MTT and Geohash)
- 🔧 Custom geospatial-temporal function mapping
- 🔍 Support for complex semantic queries with spatiotemporal constraints
- 🔌 Easy integration with existing systems

## Project Directory
- `GraST-Java`: GraST's core component, built on Maven, including geographic entity mapping across data, and conversion of Cypher queries to SQL queries
- `Data-Importer`: Import geographic entities synchronously into graph and relational databases and build indexes, based on FastAPI


## Supported Geographic Calculation Functions

GraST currently supports the following geographic calculation functions:

- `GraST.within(IDA, IDB)`
- `GraST.contains(IDA, IDB)`
- `GraST.intersects(IDA, IDB)`
- `GraST.touches(IDA, IDB)`
- `GraST.overlaps(IDA, IDB)`
- `GraST.equals(IDA, IDB)`
- `GraST.disjoint(IDA, IDB)`
- `GraST.covers(IDA, IDB)` 
- `GraST.crosses(IDA, IDB)`
- `GraST.knn(IDA, IDB, num)`
- `GraST.withinDistance(IDA, IDB, distance)`
- `GraST.withinGeom(IDA, geom:wkt)`
- `GraST.buffer(IDA, distance)`
- `GraST.area(IDA)`
- `GraST.distance(IDA, IDB)`

## Add GraST Library Extension to Neo4j

1. Copy `GraST.jar` and `database.properties` from [Link](https://github.com/GeospatialKG/GraST/releases)
2. Paste them into the `\plugins` folder of your Neo4j installation.
3. Define the PostGIS connection information in `database.properties`:
4. Open `neo4j.conf` and set the permissions for `dbms.security.procedures` (official way to enable plugin permissions):
```
dbms.security.procedures.unrestricted=....,GraST.*
dbms.security.procedures.allowlist=....,GraST.*
```
5. Using `GraST Data Importer` and import the geographic data before querying. See the next section for steps.

## Geospatial Data Importer Installation
The data importer is located in the /Data-Importer folder of the project source code and is built based on Python.

To install the required dependencies, run the following command:

```
pip install fastapi uvicorn staticfiles pygeohash geopandas pandas neo4j sqlalchemy geoalchemy2 psycopg2 gdal==3.8.4 numpy==1.26.4
```

Please note that we recommend using the specified versions for GDAL (3.8.4) to ensure compatibility.

If the installation of GDAL fails, you can manually download the appropriate wheel file from https://github.com/cgohlke/geospatial-wheels/releases and install it offline. For example:

```
pip install .\GDAL-3.8.2-cp310-cp310-win_amd64.whl
```

## Geospatial Data Importer Tutorial

1. Navigate to the `Data-Importer` directory
2. Start the FastAPI server: `uvicorn main:app --reload --timeout-keep-alive`
3. Open your web browser and navigate to `http://127.0.0.1:8000` to access the GraST data importer.

Please note:
- For vector data, compress the `.shp`, `.dbf`, and other files into a ZIP file and upload it using the Data Importer. Three sample files are provided in the `Datasets` directory: `borough.zip`, `check-ins.zip`, and `NY_POIs.zip`.<br>
  <img src="FIG/img.png" width="300">

- For raster data, simply upload the `.tif` file.<br>
  <img src="FIG/img.png" width="300">

## Geospatial Data Graph Organization Example

- The Geohash index of the geographic node:<br>
  <img src="FIG/img_2.png" width="460">

- The MTT index of the geographic node: Querying geographic nodes based on multi-granularity time trees:<br>
  <img src="FIG/img_3.png" width="460">
```cypher
MATCH (y:Year {value: 2012})<-[:isYearOf]-(m:Month {value: 4})<-[:isMonthOf]-(d:Day {value: 16})<-[:isDayOf]-(h:Hour)<-[:isHourOf]-(c:checkins)
RETURN y, m, d, h, c
```


## GraST Development

The query conversion code for GraST is located in the `GraST-Java` directory (built on Maven). Users can extend and develop it further.

After modifying the code, it needs to be packaged into a JAR file and deployed to the Neo4j plugins directory.

## Query Examples using GraST

Datasets:
- Landsat Collection 2 Tier 1 Level 2 32-Day NDVI Composite: [Link](https://developers.google.cn/earth-engine/datasets/catalog/LANDSAT_COMPOSITES_C02_T1_L2_32DAY_NDVI)
- NYC Borough Boundaries: [Link](https://data.cityofnewyork.us/)
- FourSquare - NYC Check-ins: [Link](https://www.kaggle.com/datasets/chetanism/foursquare-nyc-and-tokyo-checkin-dataset)
- New York POI Data

Next, we will demonstrate geospatial-temporal queries in Neo4j based on these datasets.

## Contributing

We welcome contributions to GraST! If you'd like to contribute, please follow the guidelines outlined in [CONTRIBUTING.md](CONTRIBUTING.md).
```
