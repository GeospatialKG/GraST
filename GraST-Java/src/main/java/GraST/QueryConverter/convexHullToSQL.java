package GraST.QueryConverter;

import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import GraST.EntityMapper.DatabaseProperties;

public class convexHullToSQL {

    @Procedure(value = "GraST.convexHull", mode = org.neo4j.procedure.Mode.READ)
    @Description("Calculates the convex hull of geometries from 'table' with specified IDs.")
    public Stream<ConvexHullOutputRecord> convexHull(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildConvexHullQuery(table, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<ConvexHullOutputRecord> results = Stream.builder();
            while (rs.next()) {
                byte[] geomBytes = rs.getBytes("convexHull"); // Assuming convexHull is returned as binary
                results.add(new ConvexHullOutputRecord(rs.getLong("id"), geomBytes));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate convex hull.", e);
        }
    }

    private String buildConvexHullQuery(String table, List<Long> ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, ST_AsText(ST_ConvexHull(geom)) AS convexHull FROM ").append(table);
        if (!ids.isEmpty()) {
            sql.append(" WHERE id IN (").append(ids.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(")");
        }
        return sql.toString();
    }

    public static class ConvexHullOutputRecord {
        public long id;
        public byte[] convexHull;

        public ConvexHullOutputRecord(long id, byte[] convexHull) {
            this.id = id;
            this.convexHull = convexHull;
        }
    }
}
