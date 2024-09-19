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

public class envelopeToSQL {

    @Procedure(value = "GraST.envelope", mode = org.neo4j.procedure.Mode.READ)
    @Description("Calculates the bounding envelope of geometries from 'table' with specified IDs.")
    public Stream<EnvelopeOutputRecord> envelope(
            @Name("table") String table,
            @Name("ids") List<Long> ids) {

        try (Connection conn = DriverManager.getConnection(
                DatabaseProperties.getDbUrl(),
                DatabaseProperties.getUser(),
                DatabaseProperties.getPassword())) {
            String sql = buildEnvelopeQuery(table, ids);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            Stream.Builder<EnvelopeOutputRecord> results = Stream.builder();
            while (rs.next()) {
                byte[] geomBytes = rs.getBytes("envelope"); // Assuming envelope is returned as binary
                results.add(new EnvelopeOutputRecord(rs.getLong("id"), geomBytes));
            }
            return results.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate envelope.", e);
        }
    }

    private String buildEnvelopeQuery(String table, List<Long> ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, ST_AsBinary(ST_Envelope(geom)) AS envelope FROM ").append(table);
        if (!ids.isEmpty()) {
            sql.append(" WHERE id IN (").append(ids.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(")");
        }
        return sql.toString();
    }

    public static class EnvelopeOutputRecord {
        public long id;
        public byte[] envelope;

        public EnvelopeOutputRecord(long id, byte[] envelope) {
            this.id = id;
            this.envelope = envelope;
        }
    }
}
