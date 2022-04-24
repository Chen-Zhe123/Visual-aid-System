package com.ipcamera.demotest.database.clouddb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.sql.DataSource;

public class DBUtils {

    private static Connection connection;
    private static DataSource dataSource;

    public static void initJDBC(){
        try {
            Properties pros = new Properties();
            // TODO 将字段写入配置文件中
//            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("jdbc.properties");
//            pros.load(is);
            pros.setProperty("name","chenzhe");
            pros.setProperty("password","@021112Cz");
            pros.setProperty("driverClass","com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://192.168.1.105:33306/myeye";
            connection = DriverManager.getConnection(url,pros);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Connection getConnection() throws Exception {
        return connection;
    }
}
