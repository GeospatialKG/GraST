package GraST.EntityMapper;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class DatabaseProperties {
    private static String dbUrl;
    private static String user;
    private static String password;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties prop = new Properties();
        // 构建相对路径
        String relativePath = "plugins/database.properties"; // 这里的路径可以根据实际情况调整
        String basePath = System.getProperty("user.dir");
        String fullPath = Paths.get(basePath, relativePath).toString();

        try (FileInputStream input = new FileInputStream(new File(fullPath))) {
            prop.load(input);
            dbUrl = prop.getProperty("database.url");
            user = prop.getProperty("database.user");
            password = prop.getProperty("database.password");
        } catch (IOException e) {
            throw new RuntimeException("Unable to load database properties from path: " + fullPath, e);
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getUser() {
        return user;
    }

    public static String getPassword() {
        return password;
    }
}
