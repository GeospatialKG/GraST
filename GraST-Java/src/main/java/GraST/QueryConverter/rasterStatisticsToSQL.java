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
import static GraST.QueryConverter.IdentifierQuoter.quoteIdentifier;

public class rasterStatisticsToSQL {

    @Procedure(value = "GraST.stats", mode = org.neo4j.procedure.Mode.READ)
    @Description("Returns statistical summary of the raster data from a raster table for a given geometry.")
    public Stream<StatsOutputRecord> stats(
            @Name("rasterTable") String rasterTable,
            @Name("wktGeom") String wktGeom) {  // Added new parameter for WKT geometry

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildStatsQuery(rasterTable, wktGeom);  // Pass the WKT geometry to the query builder
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<StatsOutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new StatsOutputRecord(
                        rs.getDouble("min"),
                        rs.getDouble("max"),
                        rs.getDouble("mean"),
                        rs.getDouble("sum"),
                        rs.getLong("count")
                ));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve raster statistics.", e);
        }
    }

    private String buildStatsQuery(String rasterTable, String wktGeom) {  // Modified method to include WKT geometry
        return "SELECT (ST_SummaryStatsAgg(ST_Clip(r.rast, ST_GeomFromText('" + wktGeom + "', 4326), true), 1, true)).* FROM " + quoteIdentifier(rasterTable) + " r " +
                "WHERE ST_Intersects(r.rast, ST_GeomFromText('" + wktGeom + "', 4326))";
    }

    /**
     * StatsOutputRecord encapsulates the result of the raster statistics query.
     * Each record contains statistical measures such as minimum, maximum, mean, sum, and count of pixel values.
     */
    public static class StatsOutputRecord {
        public double min;
        public double max;
        public double mean;
        public double sum;
        public long count;

        public StatsOutputRecord(double min, double max, double mean, double sum, long count) {
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.sum = sum;
            this.count = count;
        }
    }
}
