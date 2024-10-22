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

public class overlapsToSQL {

    @Procedure(value = "GraST.overlaps", mode = org.neo4j.procedure.Mode.READ)
    @Description("Checks if geometries from 'table1' overlap those from 'table2' in an RDBMS.")
    public Stream<OutputRecord> overlaps(
            @Name("table1") String table1,
            @Name("ids1") List<Long> ids1,
            @Name("table2") String table2,
            @Name("ids2") List<Long> ids2) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            StringBuilder sql = new StringBuilder("SELECT a.id as aid, b.id as bid ");
            sql.append("FROM ").append(quoteIdentifier(table1)).append(" a, ").append(quoteIdentifier(table2)).append(" b ");
            sql.append("WHERE ST_Overlaps(a.geom, b.geom) ");

            if (ids1 != null && !ids1.isEmpty()) {
                sql.append("AND a.id IN (").append(ids1.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
            }
            if (ids2 != null && !ids2.isEmpty()) {
                sql.append("AND b.id IN (").append(ids2.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
            }

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<OutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new OutputRecord(rs.getLong("aid"), rs.getLong("bid")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute spatial relationship query.", e);
        }
    }

    public static class OutputRecord {
        public long id1;
        public long id2;

        public OutputRecord(long id1, long id2) {
            this.id1 = id1;
            this.id2 = id2;
        }
    }
}