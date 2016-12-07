package com.linecorp.example.linebot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.io.InputStream;

import org.json.JSONArray;

public class PostgresHelper {
    
    private Connection conn;
    private String url;
    
    //we don't like this constructor
    protected PostgresHelper() {}
    
    public PostgresHelper(String url) {
        this.url = url;
    }
    
    public boolean connect() throws SQLException, ClassNotFoundException {
        if (url.isEmpty()) {
            throw new SQLException("Database credentials missing");
        }
        
        Class.forName("org.postgresql.Driver");
        this.conn = DriverManager.getConnection(this.url);
        return true;
    }
    
    public ResultSet execQuery(String query) throws SQLException {
        return this.conn.createStatement().executeQuery(query);
    }
    
    public int insert(String table, String fileName, InputStream inputStream) throws SQLException {
        
        PreparedStatement ps = conn.prepareStatement("INSERT INTO images VALUES (?, ?)");
        ps.setString(1, fileName);
        ps.setBinaryStream(2, inputStream);
        
        return ps.executeUpdate();
    }
}
