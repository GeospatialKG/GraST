package GraST.QueryConverter;

import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Stream;
import GraST.EntityMapper.DatabaseProperties;
import static GraST.QueryConverter.IdentifierQuoter.quoteIdentifier;

public class distanceToSQL {

    @Procedure(value = "GraST.distance", mode = org.neo4j.procedure.Mode.READ)
    @Description("Calculates the total distance between consecutive geometries from 'table' with specified IDs.")
    public Stream<DistanceOutputRecord> distance(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildDistanceQuery(table, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            double totalDistance = 0.0;
            while (rs.next()) {
                totalDistance += rs.getDouble("distance");
            }
            // Return the total distance
            Stream.Builder<DistanceOutputRecord> results = Stream.builder();
            results.add(new DistanceOutputRecord(totalDistance));
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate distance.", e);
        }
    }

    private String buildDistanceQuery(String table, List<Long> ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH distances AS (");

        // Loop through ids and calculate distances between consecutive points
        for (int i = 0; i < ids.size() - 1; i++) {
            long id1 = ids.get(i);
            long id2 = ids.get(i + 1);
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT ")
                    .append(id1).append(" AS id1, ").append(id2).append(" AS id2, ")
                    .append("ST_Distance(ST_Transform(a.geom, 4326)::geography, ST_Transform(b.geom, 4326)::geography) AS distance ")
                    .append("FROM ").append(quoteIdentifier(table)).append(" a, ").append(quoteIdentifier(table)).append(" b ")
                    .append("WHERE a.id = ").append(id1).append(" AND b.id = ").append(id2);
        }

        sql.append(") SELECT SUM(distance) AS distance FROM distances");
        return sql.toString();
    }


    public static class DistanceOutputRecord {
        public double totalDistance;

        public DistanceOutputRecord(double totalDistance) {
            this.totalDistance = totalDistance;
        }
    }
}
