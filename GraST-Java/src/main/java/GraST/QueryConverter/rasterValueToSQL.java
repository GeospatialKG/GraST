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
 * This class provides a Neo4j stored procedure to get the raster pixel values at specific points or all points from a raster table in an RDBMS.
 */
public class rasterValueToSQL {

    /**
     * A Neo4j stored procedure that returns the raster pixel values for specified points or all points from a raster table.
     *
     * @param pointTable The name of the table containing the point geometries.
     * @param pointIds A list of point IDs to specify which points to query. If empty, all points are queried.
     * @param rasterTable The name of the raster table containing the raster data.
     * @return A stream of ValueOutputRecord, each containing a point ID and the corresponding raster pixel value at that point.
     */
    @Procedure(value = "GraST.value", mode = org.neo4j.procedure.Mode.READ)
    @Description("Returns the raster pixel values at specific points or all points from a raster table.")
    public Stream<ValueOutputRecord> value(
            @Name("pointTable") String pointTable,
            @Name("pointIds") List<Long> pointIds,
            @Name("rasterTable") String rasterTable) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildValueQuery(pointTable, pointIds, rasterTable);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<ValueOutputRecord> results = Stream.builder();
            while (rs.next()) {
                results.add(new ValueOutputRecord(rs.getLong("id"), rs.getDouble("pixel_value")));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve raster values.", e);
        }
    }

    private String buildValueQuery(String pointTable, List<Long> pointIds, String rasterTable) {
        StringBuilder sql = new StringBuilder();
        if (pointIds != null && !pointIds.isEmpty()) {
            // 当查询特定的点时，从点表中获取 geom 和 id
            sql.append("SELECT p.id, ST_Value(r.rast, p.geom) AS pixel_value ")
                    .append("FROM ").append(rasterTable).append(" r, ")
                    .append("(SELECT id, geom FROM ").append("\"").append(pointTable).append("\"").append(" WHERE id = ")
                    .append(pointIds.get(0)).append(") AS p ")
                    .append("WHERE ST_Intersects(r.rast, p.geom)");
        } else {
            // 查询所有点，确保每个点的 id 和 geom 都被选择
            sql.append("SELECT p.id, ST_Value(r.rast, p.geom) AS pixel_value ")
                    .append("FROM ").append(quoteIdentifier(pointTable)).append(" p ")
                    .append("JOIN ").append(quoteIdentifier(rasterTable)).append(" r ON ST_Intersects(r.rast, p.geom)");
        }
        return sql.toString();
    }


    /**
     * ValueOutputRecord encapsulates the result of the raster value query.
     * Each record contains a point ID and the raster pixel value at that point.
     */
    public static class ValueOutputRecord {
        public long id;
        public double pixel_value;

        public ValueOutputRecord(long id, double pixel_value) {
            this.id = id;
            this.pixel_value = pixel_value;
        }
    }
}
