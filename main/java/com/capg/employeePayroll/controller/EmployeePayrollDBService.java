package com.capg.employeePayroll.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.capg.employeePayroll.jdbc.EmployeePayrollDBException;
import com.capg.employeePayroll.model.EmployeePayrollData;

public class EmployeePayrollDBService {
	
	public enum Operation {
		SUM, AVG, MIN, MAX, COUNT;
	}
	
	private static Logger log = Logger.getLogger(EmployeePayrollDBService.class.getName());
	
	private PreparedStatement employeePayrollDataStatement;
	private static EmployeePayrollDBService employeePayrollDBService;
	private int connectionCounter = 0;
	
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
	
	public List<EmployeePayrollData> getEmployeePayrollForDateRange(LocalDate startDate, LocalDate endDate) {
		String sql = String.format("SELECT * FROM employee_payroll WHERE startDate BETWEEN '%s' AND '%s'", Date.valueOf(startDate), Date.valueOf(endDate));
		return getEmployeePayrollAfterExecutingQuery(sql);
	}
	
	public Map<String, Double> getDataByGender(Operation operation) {
		String sql;
		String columnName;
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
		try (Connection connection = this.getConnection();
			Statement statement = connection.createStatement();) {
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
		try (Connection connection = this.getConnection();
			Statement statement = connection.createStatement();) {
			return statement.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int updateEmployeeSalaryInBothTables(String employeeName, Double salary) throws EmployeePayrollDBException {
		int employeeId = 0;
		Connection connection = null;
		int result = 0;
		try {
			connection = this.getConnection();
			connection.setAutoCommit(false);
		}
		catch (SQLException e) {
			throw new EmployeePayrollDBException("Couldn't establish connection.");
		}
		
		try (Statement statement = connection.createStatement();) {
			String sql = String.format("UPDATE employee_payroll SET salary = %.2f WHERE name = '%s';", salary, employeeName);
			int rowsAffected = statement.executeUpdate(sql);
			if(rowsAffected == 1) {
				String sql1 = String.format("SELECT id from employee_payroll WHERE name = '%s'", employeeName);
				ResultSet resultSet = statement.executeQuery(sql1);
				if(resultSet.next())
					employeeId = resultSet.getInt("id");
			}
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to update employee_payroll");
		}
		
		try (Statement statement = connection.createStatement();) {
			double deductions = salary * 0.2;
			double taxablePay = salary - deductions;
			double tax = taxablePay * 0.1;
			double netPay = salary - tax;
			String sql = String.format("UPDATE payroll_details SET basic_pay = %s, deductions = %s, taxable_pay = %s, tax = %s, net_pay = %s"
										+ " WHERE id = %s;", salary, deductions, taxablePay, tax, netPay, employeeId);
			int rowsAffected = statement.executeUpdate(sql);
			if(rowsAffected == 1) {
				connection.commit();
				result = 1;
			}	
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to update payroll_details");
		} finally {
			if(connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return result;
	}

	public EmployeePayrollData addEmployeeToEmployeePayrollTable(String name, double salary, LocalDate startDate, String gender) {
		int employeeId = -1;
		EmployeePayrollData employeePayrollData = null;
		String sql = String.format("INSERT INTO employee_payroll (name, gender, salary, startDate) " + "VALUES ('%s', '%s', '%s', '%s');", name, gender, salary, Date.valueOf(startDate));
		try (Connection connection = this.getConnection();
			Statement statement = connection.createStatement();) {
			int rowsAffected = statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
			if(rowsAffected == 1) {
				ResultSet resultSet = statement.getGeneratedKeys();
				if(resultSet.next())
					employeeId = resultSet.getInt("id");
			}
			employeePayrollData = new EmployeePayrollData(employeeId, name, salary, startDate);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return employeePayrollData;
	}
	
	public EmployeePayrollData addEmployeeToPayroll(String name, double salary, LocalDate startDate, String gender) throws EmployeePayrollDBException {
		int employeeId = -1;
		EmployeePayrollData employeePayrollData = null;
		Connection connection = null;
		try {
			connection = this.getConnection();
			connection.setAutoCommit(false);
		}
		catch (SQLException e) {
			throw new EmployeePayrollDBException("Couldn't establish connection.");
		}
		
		try (Statement statement = connection.createStatement();) {
			String sql = String.format("INSERT INTO employee_payroll (name, gender, salary, startDate) " + 
										"VALUES ('%s', '%s', '%s', '%s');", name, gender, salary, Date.valueOf(startDate));
			int rowsAffected = statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
			if(rowsAffected == 1) {
				ResultSet resultSet = statement.getGeneratedKeys();
				if(resultSet.next())
					employeeId = resultSet.getInt(1);
			}
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into employee_payroll");
		}
		
		try (Statement statement = connection.createStatement();) {
			double deductions = salary * 0.2;
			double taxable_pay = salary - deductions;
			double tax = taxable_pay * 0.1;
			double net_pay = salary - tax;
			String sql = String.format("INSERT INTO payroll_details (id, basic_pay, deductions, taxable_pay, tax, net_pay)" 
										+ " VALUES (%s, %s, %s, %s, %s, %s);", employeeId, salary, deductions, taxable_pay, tax, net_pay);
			int rowsAffected = statement.executeUpdate(sql);
			if(rowsAffected == 1) {
				employeePayrollData = new EmployeePayrollData(employeeId, name, salary, startDate);
				connection.commit();
			}	
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into payroll_details");
		} finally {
			if(connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return employeePayrollData;
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
				Date startDate = resultSet.getDate("startDate");
				list.add(new EmployeePayrollData(id, name, salary, startDate.toLocalDate()));
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	private synchronized Connection getConnection() throws SQLException {
		String dbURL = "jdbc:mysql://localhost:3306/payroll_service?useSSL=false";
		String userName = "root";
		String password = "Interference@SQL1";
		connectionCounter++;
		Connection connection;
		log.log(Level.INFO, ()-> "Processing thread: " + Thread.currentThread().getName() +" Connecting to db with id: " + connectionCounter);
		connection = DriverManager.getConnection(dbURL, userName, password);
		log.log(Level.INFO, ()-> "Processing thread: " + Thread.currentThread().getName() + " Connection to db with id: " + connectionCounter + "successful!!!!!");
		return connection;
	}
}
