package com.capg.employeePayroll.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.capg.employeePayroll.model.EmployeePayrollData;

public class EmployeePayrollDBService {
	
	public enum Operation {
		SUM, AVG, MIN, MAX, COUNT;
	}
	
	private static Logger LOG = Logger.getLogger(EmployeePayrollDBService.class.getName());
	
	private PreparedStatement employeePayrollDataStatement;
	private static EmployeePayrollDBService employeePayrollDBService;
	
	private EmployeePayrollDBService() {
	}

	public static EmployeePayrollDBService getInstance() {
		if(employeePayrollDBService == null)
			employeePayrollDBService = new EmployeePayrollDBService();
		return employeePayrollDBService;
	}
	
	public List<EmployeePayrollData> readData() {
		String sql = "SELECT * FROM employee_payroll;";
		return getEmployeePayrollAfterExecutingQuery(sql);
	}
	
	public List<EmployeePayrollData> getEmployeePayrollForDateRange(Date startDate, Date endDate) {
		String sql = String.format("SELECT * FROM employee_payroll WHERE start BETWEEN '%s' AND '%s'", startDate, endDate);
		return getEmployeePayrollAfterExecutingQuery(sql);
	}
	
	public Map<String, Double> getDataByGender(Operation operation) {
		String sql, columnName;
		switch (operation) {
			case SUM:
				sql = "SELECT gender, SUM(salary) AS Sum FROM employee_payroll GROUP BY gender;";
				columnName = "Sum";
				break;
			case AVG:
				sql = "SELECT gender, AVG(salary) AS Avg FROM employee_payroll GROUP BY gender;";
				columnName = "Avg";
				break;
			case MIN:
				sql = "SELECT gender, MIN(salary) AS Min FROM employee_payroll GROUP BY gender;";
				columnName = "Min";
				break;
			case MAX:
				sql = "SELECT gender, MAX(salary) AS Max FROM employee_payroll GROUP BY gender;";
				columnName = "Max";
				break;
			case COUNT:
				sql = "SELECT gender, COUNT(id) AS No_Of_Employees FROM employee_payroll GROUP BY gender;";
				columnName = "No_Of_Employees";
				break;
			default:
				sql = null;
				columnName = null;
				break;	
		}
		Map<String, Double> genderDataMap = new HashMap<>();
		try (Connection con = this.getConnection()) {
			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while(resultSet.next()) {
				String gender = resultSet.getString("gender");
				double data = resultSet.getDouble(columnName);
				genderDataMap.put(gender, data);
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return genderDataMap;
	}
	
	public List<EmployeePayrollData> getEmployeePayrollAfterExecutingQuery(String sql){
		List<EmployeePayrollData> employeePayrollList = new ArrayList<>();
		try (Connection con = this.getConnection()) {
			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			employeePayrollList = this.getEmployeePayrollData(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return employeePayrollList;
	}

	public int updateEmployeeData(String name, double salary) {
		return this.updateEmployeeDataUsingStatement(name,salary);
	}

	private int updateEmployeeDataUsingStatement(String name, double salary) {
		String sql = String.format("UPDATE employee_payroll set salary = %.2f where name = '%s';", salary, name);
		try (Connection connection = this.getConnection()) {
			Statement statement = connection.createStatement();
			return statement.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public List<EmployeePayrollData> getEmployeePayrollData(String name) {
		List<EmployeePayrollData> employeePayrollList = null;
		if(this.employeePayrollDataStatement == null)
			this.prepareStatementForEmployeeData();
		try {
			employeePayrollDataStatement.setString(1, name);
			ResultSet resultSet = employeePayrollDataStatement.executeQuery();
			employeePayrollList = this.getEmployeePayrollData(resultSet);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return employeePayrollList;
	}


	private void prepareStatementForEmployeeData() {
		try {
			Connection connection = this.getConnection();
			String sql = "SELECT * FROM employee_payroll WHERE name = ?";
			employeePayrollDataStatement = connection.prepareStatement(sql);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

	private List<EmployeePayrollData> getEmployeePayrollData(ResultSet resultSet) {
		List<EmployeePayrollData> list = new ArrayList<>();
		try {
			while(resultSet.next()) {
				int id = resultSet.getInt("id");
				String name = resultSet.getString("name");
				double salary = resultSet.getDouble("salary");
				Date startDate = resultSet.getDate("start");
				list.add(new EmployeePayrollData(id, name, salary, startDate));
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	private Connection getConnection() throws SQLException {
		String dbURL = "jdbc:mysql://localhost:3306/payroll_service?useSSL=false";
		String userName = "root";
		String password = "Interference@SQL1";
		Connection connection = null;
		LOG.log(Level.INFO, "Connecting to database : "+dbURL);
		connection = DriverManager.getConnection(dbURL, userName, password);
		LOG.log(Level.INFO, "Connection Successful : "+connection);
		return connection;
	}
}
