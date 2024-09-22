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

/**
 * This class provides a Neo4j stored procedure to calculate the length of geometries from a table in an RDBMS.
 */
public class lengthToSQL {

    /**
     * A Neo4j stored procedure that calculates the length of geometries from a specified table and IDs.
     *
     * @param table The name of the table containing the geometries.
     * @param ids A list of identifiers to specify which geometries to calculate the length for. If empty, calculates for all geometries in the table.
     * @return A stream of LengthOutputRecord, each containing an ID and the corresponding length of the geometry.
     */
    @Procedure(value = "GraST.length", mode = org.neo4j.procedure.Mode.READ)
    @Description("Calculates the length of geometries from 'table' with specified IDs.")
    public Stream<LengthOutputRecord> length(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildLengthQuery(table, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<LengthOutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new LengthOutputRecord(rs.getLong("id"), rs.getDouble("length")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate length.", e);
        }
    }

    private String buildLengthQuery(String table, List<Long> ids) {
        StringBuilder sql = new StringBuilder();
        StringBuilder append = sql.append("SELECT id, ST_LengthSpheroid(geom, 'SPHEROID[\"WGS 84\",6378137,298.257223563]') AS length FROM ").append(quoteIdentifier(table));
        if (ids != null && !ids.isEmpty()) {
            sql.append(" WHERE id IN (").append(ids.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(")");
        }
        return sql.toString();
    }

    /**
     * LengthOutputRecord encapsulates the result of the length query.
     * Each record contains an ID and the length of the geometry associated with that ID.
     */
    public static class LengthOutputRecord {
        public long id;
        public double length;

        public LengthOutputRecord(long id, double length) {
            this.id = id;
            this.length = length;
        }
    }
}
