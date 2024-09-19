package GraST.EntityMapper;

import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import java.util.stream.Stream;

public class DatabaseInfo {

    @Procedure(value = "GraST.DBInfo", mode = org.neo4j.procedure.Mode.READ)
    @Description("Prints database connection info")
    public Stream<DatabaseInfoResult> logDatabaseInfo() {
        System.out.println("Database URL: " + DatabaseProperties.getDbUrl());
        System.out.println("Database User: " + DatabaseProperties.getUser());
        System.out.println("Database Password: " + DatabaseProperties.getPassword());

        // 创建输出流以便可以从Neo4j中获取这些值
        DatabaseInfoResult result = new DatabaseInfoResult();
        result.dbUrl = DatabaseProperties.getDbUrl();
        result.dbUser = DatabaseProperties.getUser();
        result.dbPassword = DatabaseProperties.getPassword();
        return Stream.of(result);
    }

    // 用于返回数据库信息的简单类
    public static class DatabaseInfoResult {
        public String dbUrl;
        public String dbUser;
        public String dbPassword;
    }

    public static void main(String[] args) {
        DatabaseInfo printer = new DatabaseInfo();
        printer.logDatabaseInfo().forEach(result -> {
            System.out.println("Database URL: " + result.dbUrl);
            System.out.println("Database User: " + result.dbUser);
            System.out.println("Database Password: " + result.dbPassword);
        });
    }
}
