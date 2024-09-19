package GraST.QueryConverter;

import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import GraST.EntityMapper.DatabaseProperties;

public class bufferGeomToSQL {

    @Procedure(value = "GraST.bufferGeometry", mode = org.neo4j.procedure.Mode.READ)
    @Description("Creates a unioned buffer geometry for specified IDs from a PostGIS table within a given distance.")
    public Stream<BufferOutputRecord> bufferGeometry(
            @Name("table") String table,
            @Name("ids") List<Long> ids,
            @Name("distance") double distance) {

        String sql = buildBufferQuery(table, ids, distance);

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword());
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            String geomUnion = null;
            if (rs.next()) {
                geomUnion = rs.getString("geom_union");
            }
            Stream.Builder<BufferOutputRecord> results = Stream.builder();
            results.add(new BufferOutputRecord(geomUnion));
            return results.build();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create buffer geometry.", e);
        }
    }

    private String buildBufferQuery(String table, List<Long> ids, double distance) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ST_AsText(ST_Union(ST_Transform(ST_Buffer(ST_Transform(geom, 3857), ");
        sql.append(distance);
        sql.append("), 4326))) AS geom_union ");
        sql.append("FROM \"").append(table).append("\" ");
        sql.append("WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append(ids.get(i));
            if (i < ids.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(");");
        return sql.toString();
    }


    public static class BufferOutputRecord {
        public String bufferGeometry;

        public BufferOutputRecord(String bufferGeometry) {
            this.bufferGeometry = bufferGeometry;
        }
    }
}
