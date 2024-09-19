import pygeohash as pgh


def cal_geohash(geom, precision=6):
    unique_geohashes = set()
    if geom.geom_type == 'Point':
        unique_geohashes.add(pgh.encode(geom.y, geom.x, precision))
    elif geom.geom_type == 'MultiPoint':
        for point in geom:
            unique_geohashes.add(pgh.encode(point.y, point.x, precision))
    elif geom.geom_type in ['LineString', 'MultiLineString']:
        if geom.geom_type == 'LineString':
            lines = [geom]
        else:
            lines = geom.geoms
        for line in lines:
            bounds = line.bounds
            corners = [(bounds[1], bounds[0]), (bounds[1], bounds[2]), (bounds[3], bounds[0]), (bounds[3], bounds[2])]
            for (lat, lon) in corners:
                unique_geohashes.add(pgh.encode(lat, lon, precision))
    elif geom.geom_type in ['Polygon', 'MultiPolygon']:
        if geom.geom_type == 'Polygon':
            polygons = [geom]
        else:
            polygons = geom.geoms
        for poly in polygons:
            bounds = poly.bounds
            corners = [(bounds[1], bounds[0]), (bounds[1], bounds[2]), (bounds[3], bounds[0]), (bounds[3], bounds[2])]
            for (lat, lon) in corners:
                unique_geohashes.add(pgh.encode(lat, lon, precision))

    return list(unique_geohashes)