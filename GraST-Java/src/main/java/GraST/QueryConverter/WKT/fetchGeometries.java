package GraST.QueryConverter.WKT;

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

public class fetchGeometries {

    @Procedure(value = "GraST.geometry", mode = org.neo4j.procedure.Mode.READ)
    @Description("Fetches id and geometry from a specified table by id list or fetches all if id list is empty.")
    public Stream<GeomRecord> fetchWKT(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {

            StringBuilder sql = new StringBuilder("SELECT id, ST_AsText(geom) as geom FROM \"")
                    .append(table).append("\"");

            // Append WHERE clause if ids are provided
            if (ids != null && !ids.isEmpty()) {
                String idList = ids.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                sql.append(" WHERE id IN (").append(idList).append(")");
            }

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            ResultSet rs = stmt.executeQuery();

            Stream.Builder<GeomRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new GeomRecord(rs.getLong("id"), rs.getString("geom")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query to fetch geometries.", e);
        }
    }

    public static class GeomRecord {
        public long id;
        public String geom;

        public GeomRecord(long id, String geom) {
            this.id = id;
            this.geom = geom;
        }
    }
}
