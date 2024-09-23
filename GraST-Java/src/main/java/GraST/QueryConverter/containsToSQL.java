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
 * This class facilitates spatial analysis by determining whether the geometries in one table contain those of another table in a relational database system (RDBMS).
 * It utilizes SQL and GIS capabilities of the underlying RDBMS (PostGIS in PostgreSQL) to perform the spatial query efficiently.
 */
public class containsToSQL {

    /**
     * A Neo4j stored procedure that dynamically checks if geometries from 'table1' contain those from 'table2'.
     * This procedure is intended to be used where the data regarding spatial locations is stored in an RDBMS and needs to be accessible via Neo4j.
     *
     * @param table1 The name of the first table containing geometries to check.
     * @param ids1 A list of identifiers from table1 to narrow down the query for efficiency reasons. If empty, all geometries are checked.
     * @param table2 The name of the second table containing reference geometries to check against.
     * @param ids2 A list of identifiers from table2 to narrow down the query. If empty, all geometries in table2 are used as reference.
     * @return A stream of OutputRecord, each indicating a pair of IDs from both tables where the geometry from table1 contains that of table2.
     */
    @Procedure(value = "GraST.contains", mode = org.neo4j.procedure.Mode.READ)
    @Description("Checks if geometries from 'table1' contain those from 'table2' in an RDBMS.")
    public Stream<OutputRecord> contains(
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
            sql.append("WHERE ST_Contains(a.geom, b.geom) ");

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

    /**
     * OutputRecord encapsulates the result of the spatial query.
     * Each record represents a pair of geometries from the two specified tables where the geometry from table1 contains that of table2.
     *
     * @param id1 The ID of the geometry from table1.
     * @param id2 The ID of the geometry from table2.
     */
    public static class OutputRecord {
        public long id1;
        public long id2;

        public OutputRecord(long id1, long id2) {
            this.id1 = id1;
            this.id2 = id2;
        }
    }
}