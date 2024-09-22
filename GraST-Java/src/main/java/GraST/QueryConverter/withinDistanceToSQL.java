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

public class withinDistanceToSQL {

    @Procedure(value = "GraST.withinDistance", mode = org.neo4j.procedure.Mode.READ)
    @Description("Identifies geometries from 'table2' within a specified buffer distance around geometries in 'table1' and returns IDs from both tables.")
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
                results.add(new DistanceOutputRecord(rs.getLong("id1"), rs.getLong("id2")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute within distance query.", e);
        }
    }

    private String buildWithinDistanceQuery(String table1, List<Long> ids1, double distance, String table2, List<Long> ids2) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id AS id1, b.id AS id2 ")
                .append("FROM ").append("\"").append(table1).append("\"").append(" a, ")
                .append("\"").append(table2).append("\"").append(" b ") // 确保两个表名都有双引号
                .append("WHERE ST_DistanceSphere(ST_SetSRID(a.geom, 4326), ST_SetSRID(b.geom, 4326)) <= ? ");  // 使用 ? 作为参数占位符
        if (!ids1.isEmpty()) {
            sql.append("AND a.id IN (").append(ids1.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
        }

        if (ids2 != null && !ids2.isEmpty()) {
            sql.append("AND b.id IN (").append(ids2.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
        }

        return sql.toString();
    }


    public static class DistanceOutputRecord {
        public long id1; // ID from table1
        public long id2; // ID from table2

        public DistanceOutputRecord(long id1, long id2) {
            this.id1 = id1;
            this.id2 = id2;
        }
    }
}
