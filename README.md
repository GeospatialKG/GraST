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

public class knnToSQL {

    @Procedure(value = "GraST.knn", mode = org.neo4j.procedure.Mode.READ)
    @Description("Performs a K-Nearest Neighbors (KNN) search using specified IDs from 'table1' against geometries from 'table2'.")
    public Stream<KnnOutputRecord> knn(
            @Name("table1") String table1,
            @Name("ids1") List<Long> ids1,
            @Name("table2") String table2,
            @Name("ids2") List<Long> ids2,
            @Name("k") long k) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildKnnQuery(table1, ids1, table2, ids2, k);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<KnnOutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new KnnOutputRecord(rs.getLong("id1"), rs.getLong("id2"), rs.getDouble("distance")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute KNN query.", e);
        }
    }

    private String buildKnnQuery(String table1, List<Long> ids1, String table2, List<Long> ids2, long k) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id AS id1, b.id AS id2, ST_DistanceSphere(ST_SetSRID(a.geom, 4326), ST_SetSRID(b.geom, 4326)) AS distance ");
        sql.append("FROM ").append(quoteIdentifier(table1)).append(" a, ").append(quoteIdentifier(table2)).append(" b ");

        if (!ids1.isEmpty()) {
            sql.append("WHERE a.id IN (").append(ids1.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
        }

        if (!ids2.isEmpty()) {
            if (!ids1.isEmpty()) {
                sql.append("AND ");
            } else {
                sql.append("WHERE ");
            }
            sql.append("b.id IN (").append(ids2.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
        }

        sql.append("ORDER BY ST_DistanceSphere(ST_SetSRID(a.geom, 4326), ST_SetSRID(b.geom, 4326)) LIMIT ").append(k);

        return sql.toString();
    }

    public static class KnnOutputRecord {
        public long id1;
        public long id2;
        public double distance;

        public KnnOutputRecord(long id1, long id2, double distance) {
            this.id1 = id1;
            this.id2 = id2;
            this.distance = distance;
        }
    }
}