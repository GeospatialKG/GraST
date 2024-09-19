package GraST.QueryConverter;

import GraST.EntityMapper.DatabaseProperties;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

public class LineCreator {

    @Procedure(value = "GraST.createLineFromCheckin", mode = org.neo4j.procedure.Mode.WRITE)
    @Description("Creates a LineString from geometries in 'Checkin' table using provided IDs and inserts it into the 'Line' table.")
    public void createLineFromCheckin(
            @Name("checkinTable") String checkinTable,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {

            // Create the SQL statement for inserting the LineString into the Line table
            String sql = buildCreateLineQuery(checkinTable, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();  // Execute the insert operation

        } catch (Exception e) {
            throw new RuntimeException("Failed to create line from Checkin data.", e);
        }
    }

    private String buildCreateLineQuery(String checkinTable, List<Long> ids) {
        StringBuilder sql = new StringBuilder();

        // Build the query to create the LineString from the geometries in Checkin table
        sql.append("INSERT INTO Line (geom) ")
                .append("SELECT ST_MakeLine(geom ORDER BY id) ")
                .append("FROM ").append(quoteIdentifier(checkinTable)).append(" ")
                .append("WHERE id IN (")
                .append(ids.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                .append(") ");

        return sql.toString();
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\""; // Properly escape quotes by doubling them
    }
}
