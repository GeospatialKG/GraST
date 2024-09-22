package GraST.QueryConverter;

public class IdentifierQuoter {
    public static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}