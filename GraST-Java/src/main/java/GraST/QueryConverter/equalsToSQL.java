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
 * This class facilitates the spatial analysis by determining whether the geometries in one table are exactly equal to those of another table in a relational database system (RDBMS).
 * It utilizes SQL and GIS capabilities of the underlying RDBMS (PostGIS in PostgreSQL) to perform the spatial query efficiently.
 */
public class equalsToSQL {

    /**
     * A Neo4j stored procedure that dynamically checks if geometries from 'table1' are exactly equal to those from 'table2'.
     * This procedure is intended to be used where the data regarding spatial locations is stored in an RDBMS and needs to be accessible via Neo4j.
     *
     * @param table1 The name of the first table containing geometries to check.
     * @param ids1 A list of identifiers from table1 to narrow down the query for efficiency reasons. If empty, all geometries are checked.
     * @param table2 The name of the second table containing reference geometries to check against.
     * @param ids2 A list of identifiers from table2 to narrow down the query. If empty, all geometries in table2 are used as reference.
     * @return A stream of OutputRecord, each indicating a pair of IDs from both tables and whether the geometry from table1 is exactly equal to that of table2.
     */
    @Procedure(value = "GraST.equals", mode = org.neo4j.procedure.Mode.READ)
    @Description("Checks if geometries from 'table1' are exactly equal to those from 'table2' in an RDBMS.")
    public Stream<OutputRecord> equals(
            @Name("table1") String table1,
            @Name("ids1") List<Long> ids1,
            @Name("table2") String table2,
            @Name("ids2") List<Long> ids2) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            StringBuilder sql = new StringBuilder("SELECT a.id as aid, b.id as bid, ST_Equals(a.geom, b.geom) AS isRelated ");
            sql.append("FROM ").append(quoteIdentifier(table1)).append(" a, ").append(quoteIdentifier(table2)).append(" b ");

            if (ids1 != null && !ids1.isEmpty()) {
                sql.append("WHERE a.id IN (").append(ids1.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
            }
            if (ids2 != null && !ids2.isEmpty()) {
                sql.append(ids1 != null && !ids1.isEmpty() ? "AND " : "WHERE ");
                sql.append("b.id IN (").append(ids2.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") ");
            }

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<OutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new OutputRecord(rs.getLong("aid"), rs.getLong("bid"), rs.getBoolean("isRelated")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute spatial relationship query.", e);
        }
    }

    /**
     * OutputRecord encapsulates the result of the spatial query.
     * Each record represents a relationship check between a pair of geometries from the two specified tables.
     *
     * @param id1 The ID of the geometry from table1 involved in the check.
     * @param id2 The ID of the geometry from table2 involved in the check.
     * @param isRelated True if the geometry from table1 is exactly equal to the geometry from table2, otherwise false.
     */
    public static class OutputRecord {
        public long id1;
        public long id2;
        public boolean isRelated;

        public OutputRecord(long id1, long id2, boolean isRelated) {
            this.id1 = id1;
            this.id2 = id2;
            this.isRelated = isRelated;
        }
    }
}
