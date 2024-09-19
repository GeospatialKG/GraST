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

public class intersectsWKT {

    @Procedure(value = "GraST.intersectsWKT", mode = org.neo4j.procedure.Mode.READ)
    @Description("Checks if geometries from 'table1' intersect with a provided geometry WKT in an RDBMS.")
    public Stream<OutputRecord> intersects(
            @Name("table1") String table1,
            @Name("ids1") List<Long> ids1,
            @Name("geometryWKT") String geometryWKT) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {

            StringBuilder sql = new StringBuilder("SELECT a.id as aid, ST_Intersects(ST_SetSRID(a.geom, 4326), ST_SetSRID(ST_GeomFromText(?), 4326)) AS isRelated ");
            sql.append("FROM ").append("\"").append(table1).append("\"").append(" a ");

            if (ids1 != null && !ids1.isEmpty()) {
                sql.append("WHERE a.id IN (").append(ids1.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
            }

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            stmt.setString(1, geometryWKT);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<OutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new OutputRecord(rs.getLong("aid"), rs.getBoolean("isRelated")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute spatial relationship query.", e);
        }
    }

    public static class OutputRecord {
        public long id1;
        public boolean isRelated;

        public OutputRecord(long id1, boolean isRelated) {
            this.id1 = id1;
            this.isRelated = isRelated;
        }
    }
}
