package GraST.QueryConverter;

import GraST.EntityMapper.DatabaseProperties;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class withinDistanceGeomToSQL {

    @Procedure(value = "GraST.withinDistanceGeom", mode = org.neo4j.procedure.Mode.READ)
    @Description("Identifies geometries from 'table2' within a specified buffer distance around geometries in 'table1' and returns IDs from table1.")
    public Stream<DistanceOutputRecord> withinDistance(
            @Name("table1") String table1,
            @Name("ids1") List<Long> ids1,
            @Name("distance") Double distance,
            @Name("table2") String table2,
            @Name("ids2") List<Long> ids2) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildWithinDistanceQuery(table1, ids1, distance, table2, ids2);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDouble(1, distance); // Set the buffer distance parameter
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<DistanceOutputRecord> results = Stream.builder();
            while (rs.next()) {
                long id1 = rs.getLong("id1");
                results.add(new DistanceOutputRecord(id1));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute within distance query.", e);
        }
    }

    private String buildWithinDistanceQuery(String table1, List<Long> ids1, double distance, String table2, List<Long> ids2) {
        StringBuilder sql = new StringBuilder();

        // Collect the geometries from the "table2" into a MULTIPOINT geometry
        sql.append("WITH collected_geoms AS (")
                .append("  SELECT ST_Collect(b.geom) AS multi_geom ")
                .append("  FROM ").append(quoteIdentifier(table2)).append(" b ");

        if (ids2 != null && !ids2.isEmpty()) {
            sql.append("  WHERE b.id IN (")
                    .append(ids2.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                    .append(") ");
        }

        sql.append(") ");

        // Select the id1s from table1 that are within the distance to the MULTIPOINT
        sql.append("SELECT a.id AS id1 ")
                .append("FROM ").append(quoteIdentifier(table1)).append(" a, collected_geoms ")
                .append("WHERE ST_DistanceSphere(ST_SetSRID(a.geom, 4326), ST_SetSRID(multi_geom, 4326)) <= ? ");

        if (!ids1.isEmpty()) {
            sql.append("AND a.id IN (")
                    .append(ids1.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                    .append(") ");
        }

        return sql.toString();
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\""; // Properly escape quotes by doubling them
    }

    public static class DistanceOutputRecord {
        public long id1; // ID from table1

        public DistanceOutputRecord(long id1) {
            this.id1 = id1;
        }
    }
}
