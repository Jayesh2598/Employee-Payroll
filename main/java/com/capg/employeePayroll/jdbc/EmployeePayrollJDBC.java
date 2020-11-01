package com.capg.employeePayroll.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class EmployeePayrollJDBC {

	public static void main(String[] args) {
		String dbURL = "jdbc:mysql://localhost:3306/payroll_service?useSSL=false";
		String userName = "root";
		String password = "Interference@SQL1";
		Connection connection;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Driver loaded!");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Couldn't find driver in classpath",e);
		}
		
		listDrivers();
		
		try {
			System.out.println("Connecting to database: " + dbURL);
			connection = DriverManager.getConnection(dbURL,userName,password);
			System.out.println("Connection successful! "+connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void listDrivers() {
		Enumeration<Driver> driverList = DriverManager.getDrivers();
		while(driverList.hasMoreElements()) {
			Driver driverClass = driverList.nextElement();
			System.out.println(driverClass.getClass().getName());
		}
	}

}
