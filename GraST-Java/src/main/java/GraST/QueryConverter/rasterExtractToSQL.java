package GraST.QueryConverter;

import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.stream.Stream;
import GraST.EntityMapper.DatabaseProperties;

public class rasterExtractToSQL {

    @Procedure(value = "GraST.rasterExtractor", mode = org.neo4j.procedure.Mode.READ)
    @Description("Extracts raster regions from a specified table where raster values fall within a specified range.")
    public Stream<RasterOutputRecord> extractRasterRegionsByValue(
            @Name("rasterTable") String rasterTable,
            @Name("minValue") Double minValue,
            @Name("maxValue") Double maxValue) {

        Stream.Builder<RasterOutputRecord> results = Stream.builder();
        String sql =
                "WITH dumped AS (SELECT (ST_DumpAsPolygons(rast)).* " +
                        "FROM " + rasterTable + ") " +
                        "SELECT geom, val " +
                        "FROM dumped " +
                        "WHERE val BETWEEN ? AND ?";

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, minValue);
            stmt.setDouble(2, maxValue);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String geom = rs.getString("geom"); // Geometry as WKT or similar
                double value = rs.getDouble("val"); // The raster value
                results.add(new RasterOutputRecord(geom, value));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract raster regions by value.", e);
        }
        return results.build();
    }

    public static class RasterOutputRecord {
        public String geom;
        public double value;

        public RasterOutputRecord(String geom, double value) {
            this.geom = geom;
            this.value = value;
        }
    }
}
