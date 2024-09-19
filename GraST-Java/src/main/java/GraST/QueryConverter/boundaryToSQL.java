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

public class boundaryToSQL {

    @Procedure(value = "GraST.boundary", mode = org.neo4j.procedure.Mode.READ)
    @Description("Calculates the boundary of geometries from 'table' with specified IDs.")
    public Stream<BoundaryOutputRecord> boundary(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildBoundaryQuery(table, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<BoundaryOutputRecord> results = Stream.builder();
            while (rs.next()) {
                byte[] geomBytes = rs.getBytes("boundary"); // Assuming boundary is returned as binary
                results.add(new BoundaryOutputRecord(rs.getLong("id"), geomBytes));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate boundary.", e);
        }
    }

    private String buildBoundaryQuery(String table, List<Long> ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, ST_AsText(ST_Boundary(geom)) AS boundary FROM ").append(table);
        if (!ids.isEmpty()) {
            sql.append(" WHERE id IN (").append(ids.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(")");
        }
        return sql.toString();
    }

    public static class BoundaryOutputRecord {
        public long id;
        public byte[] boundary;

        public BoundaryOutputRecord(long id, byte[] boundary) {
            this.id = id;
            this.boundary = boundary;
        }
    }
}
