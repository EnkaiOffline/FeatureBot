package org.enkai.featurebot.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
//Тимчасова затичка для дата леэра, поки не прикручу хібернейт
public class SimpleData {

    private static final SimpleData instance = new SimpleData();
    private static final String CONNECTION_STRING = "jdbc:postgresql://ec2-54-73-22-169.eu-west-1.compute.amazonaws.com:5432/dej4tsu68hcpra?password=ee2e788c7621422c08efcb442231c99bd2711c9e98a53db1d1bc9ece584025fb&sslmode=require&user=lvzngugstpjqfu";

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    private Connection connection;

    public static SimpleData getInstance() {
        return instance;
    }

    private SimpleData() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager
                    .getConnection(CONNECTION_STRING);
            log.info("Opened database successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }

    private void checkConnection() throws SQLException {
        if(connection.isClosed()) {
            log.info("Updating database connection");
            connection = DriverManager
                    .getConnection(CONNECTION_STRING);
        }
    }
    
    public synchronized String get(String key) {
        String sql = "SELECT value FROM alldata WHERE key='" + key + "';";
        try {
            checkConnection();
            Statement statement = connection.createStatement();
            statement.executeQuery(sql);
            ResultSet rs = statement.executeQuery(sql);
            rs.next();
            String result = rs.getString("value");
            log.info(sql + " expression successful, result: " +  result);
            return result;
        } catch (SQLException exception) {
            log.error(sql + " expression failed, " + exception.getMessage());
            exception.printStackTrace();
            return null;
        }
    }

    public synchronized void put(String key, String value) {
        String sql = "INSERT INTO alldata VALUES ('" + key + "', '" + value + "') ON CONFLICT (key) DO UPDATE SET value='" + value + "'";
        try {
            checkConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
            log.info(sql + " expression successful");
        } catch (SQLException exception) {
            log.error(sql + " expression failed, " + exception.getMessage());
            exception.printStackTrace();
        }
    }
    

}
