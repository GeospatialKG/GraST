# GraST: Geospatial-Temporal Semantic Query Optimization

GraST is a high-performance framework for managing and querying geospatial-temporal data with rich semantic associations in knowledge graphs.

## Features

- Hybrid storage leveraging both graph and relational databases
- Efficient spatiotemporal indexing (MTT and Geohash)
- Custom geospatial-temporal function mapping
- Support for complex semantic queries with spatiotemporal constraints
- Easy integration with existing systems

## Installation

To install the required dependencies, run the following command:

pip install fastapi uvicorn staticfiles pygeohash geopandas pandas neo4j sqlalchemy geoalchemy2 psycopg2 gdal==3.8.4 numpy==1.26.4

Please note that we recommend using the specified versions for GDAL (3.8.4) and numpy (1.26.4) to ensure compatibility.

If the installation of GDAL fails, you can manually download the appropriate wheel file from https://github.com/cgohlke/geospatial-wheels/releases and install it offline. For example:

pip install .\GDAL-3.8.2-cp310-cp310-win_amd64.whl



## Quick Start

1. Clone the repository
2. Start the FastAPI server: uvicorn main:app --reload --timeout-keep-alive
3. Open your web browser and navigate to `http://127.0.0.1:8000` to access the GraST data importer.

For more information on using GraST, please refer to the documentation.

## Contributing

We welcome contributions to GraST! If you'd like to contribute, please follow the guidelines outlined in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.