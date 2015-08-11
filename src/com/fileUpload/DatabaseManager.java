package com.fileUpload;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
	
	private static Connection conn;
	
	public static Connection getConnection() throws SQLException, ClassNotFoundException {
		if (conn == null) {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/file_upload", "root", "gtveren45");
		}
		return conn;
	}

}
