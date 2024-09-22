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
import static GraST.QueryConverter.IdentifierQuoter.quoteIdentifier;

public class areaToSQL {

    @Procedure(value = "GraST.area", mode = org.neo4j.procedure.Mode.READ)
    @Description("Calculates the area of geometries from 'table' with specified IDs.")
    public Stream<AreaOutputRecord> area(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildAreaQuery(table, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<AreaOutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new AreaOutputRecord(rs.getLong("id"), rs.getDouble("area")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate area.", e);
        }
    }

    private String buildAreaQuery(String table, List<Long> ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, ST_Area(geom::geography) AS area FROM ").append(quoteIdentifier(table));
        if (!ids.isEmpty()) {
            sql.append(" WHERE id IN (").append(ids.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(")");
        }
        return sql.toString();
    }

    public static class AreaOutputRecord {
        public long id;
        public double area;

        public AreaOutputRecord(long id, double area) {
            this.id = id;
            this.area = area;
        }
    }
}
