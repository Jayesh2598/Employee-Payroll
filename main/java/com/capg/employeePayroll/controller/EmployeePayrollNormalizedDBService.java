package com.capg.employeePayroll.controller;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.capg.employeePayroll.controller.EmployeePayrollDBService.Operation;
import com.capg.employeePayroll.jdbc.EmployeePayrollDBException;
import com.capg.employeePayroll.model.EmployeePayrollData;

public class EmployeePayrollNormalizedDBService {

	private static Logger log = Logger.getLogger(EmployeePayrollNormalizedDBService.class.getName());

	private PreparedStatement employeePayrollNormalizedDataPreparedStatement;
	private static EmployeePayrollNormalizedDBService employeePayrollNormalizedDBService;

	private final String generalSql = "select e.employee_id, e.name, e.gender, p.basic_pay as salary, e.startDate, c.company_id, c.company_name, d.department_name "
										+ "from company c inner join employee e on c.company_id = e.company_id "
										+ "inner join payroll p on p.employee_id = e.employee_id "
										+ "inner join employee_department ed on e.employee_id = ed.employee_id "
										+ "inner join department d on d.department_id = ed.department_id";

	private EmployeePayrollNormalizedDBService() {
		// Doesn't allow other classes to instantiate this class
	}

	public static EmployeePayrollNormalizedDBService getInstance() {
		if (employeePayrollNormalizedDBService == null)
			employeePayrollNormalizedDBService = new EmployeePayrollNormalizedDBService();
		return employeePayrollNormalizedDBService;
	}

	public List<EmployeePayrollData> readData() {
		String sql = generalSql + ";";
		return getEmployeePayrollAfterExecutingQuery(sql);
	}

	public List<EmployeePayrollData> getEmployeePayrollForDateRange(LocalDate startDate, LocalDate endDate) {
		String sql = String.format((generalSql + " WHERE is_active = true AND startDate BETWEEN '%s' AND '%s';"), Date.valueOf(startDate), Date.valueOf(endDate));
		return getEmployeePayrollAfterExecutingQuery(sql);
	}

	public Map<String, Double> getDataByGender(Operation operation) {
		String sql;
		String columnName;
		switch (operation) {
		case SUM:
			sql = "SELECT e.gender, SUM(p.basic_pay) AS Sum FROM employee e inner join payroll p on e.employee_id = p.employee_id "
					+ "WHERE e.is_active = true GROUP BY e.gender;";
			columnName = "Sum";
			break;
		case AVG:
			sql = "SELECT e.gender, AVG(p.basic_pay) AS Avg FROM employee e inner join payroll p on e.employee_id = p.employee_id "
					+ "WHERE e.is_active = true GROUP BY e.gender;";
			columnName = "Avg";
			break;
		case MIN:
			sql = "SELECT e.gender, MIN(p.basic_pay) AS Min FROM employee e inner join payroll p on e.employee_id = p.employee_id "
					+ "WHERE e.is_active = true GROUP BY e.gender;";
			columnName = "Min";
			break;
		case MAX:
			sql = "SELECT e.gender, Max(p.basic_pay) AS Max FROM employee e inner join payroll p on e.employee_id = p.employee_id "
					+ "WHERE e.is_active = true GROUP BY e.gender;";
			columnName = "Max";
			break;
		case COUNT:
			sql = "SELECT gender, COUNT(employee_id) AS No_Of_Employees FROM employee WHERE is_active = true  GROUP BY gender;";
			columnName = "No_Of_Employees";
			break;
		default:
			sql = null;
			columnName = null;
			break;
		}
		Map<String, Double> genderDataMap = new HashMap<>();
		try (Connection con = this.getConnection();
			Statement statement = con.createStatement();) {
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				String gender = resultSet.getString("gender");
				double data = resultSet.getDouble(columnName);
				genderDataMap.put(gender, data);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return genderDataMap;
	}

	public int updateEmployeeData(String name, double salary) {
		double deductions = salary * 0.2;
		double taxablepay = salary - deductions;
		double tax = taxablepay * 0.1;
		double netpay = salary - tax;
		int id = 0;
		String sql1 = String.format("select employee_id from employee where name = '%s';", name);
		try (Connection connection = this.getConnection(); 
			Statement statement = connection.createStatement();) {
			ResultSet resultSet = statement.executeQuery(sql1);
			if (resultSet.next())
				id = resultSet.getInt("employee_id");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String sql2 = String.format("UPDATE payroll SET basic_pay = %s, deductions = %s, taxable_pay = %s, tax = %s, net_pay = %s WHERE employee_id = %s;",
									salary, deductions, taxablepay, tax, netpay, id);
		try (Connection connection = this.getConnection(); 
			Statement statement = connection.createStatement();) {
			return statement.executeUpdate(sql2);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public List<EmployeePayrollData> getEmployeePayrollAfterExecutingQuery(String sql) {
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

	public List<EmployeePayrollData> getEmployeePayrollData(String name) {
		List<EmployeePayrollData> employeePayrollList = null;
		if (this.employeePayrollNormalizedDataPreparedStatement == null)
			this.prepareStatementForEmployeeData();
		try {
			employeePayrollNormalizedDataPreparedStatement.setString(1, name);
			ResultSet resultSet = employeePayrollNormalizedDataPreparedStatement.executeQuery();
			employeePayrollList = this.getEmployeePayrollData(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return employeePayrollList;
	}

	private void prepareStatementForEmployeeData() {
		try {
			Connection connection = this.getConnection();
			String sql = generalSql + " WHERE name = ?;";
			employeePayrollNormalizedDataPreparedStatement = connection.prepareStatement(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private List<EmployeePayrollData> getEmployeePayrollData(ResultSet resultSet) {
		List<EmployeePayrollData> list = new ArrayList<>();
		try {
			while (resultSet.next()) {
				int id = resultSet.getInt("employee_id");
				String name = resultSet.getString("name");
				String gender = resultSet.getString("gender");
				double salary = resultSet.getDouble("salary");
				Date startDate = resultSet.getDate("startDate");
				int companyId = resultSet.getInt("company_id");
				String companyName = resultSet.getString("company_name");
				String dept = resultSet.getString("department_name");
				EmployeePayrollData obj = new EmployeePayrollData(id, name, salary, startDate.toLocalDate(), gender, companyName, companyId);
				List<String> deptList = new ArrayList<>();
				if (list.contains(obj)) {
					obj.departmentList.add(dept);
				} else {
					deptList.add(dept);
					list.add(new EmployeePayrollData(id, name, salary, startDate.toLocalDate(), gender, companyName, companyId, deptList));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	private Connection getConnection() throws SQLException {
		String dbURL = "jdbc:mysql://localhost:3306/payroll_service?useSSL=false";
		String userName = "root";
		String password = "Interference@SQL1";
		Connection connection;
		log.log(Level.INFO, () -> "Connecting to database : " + dbURL);
		connection = DriverManager.getConnection(dbURL, userName, password);
		log.log(Level.INFO, () -> "Connection successful : " + connection);
		return connection;
	}

	public EmployeePayrollData addEmployeeToPayroll(String name, String gender, String address, String phNo,
			double salary, LocalDate startDate, int companyId, String companyName, String departmentName, int departmentId)
			throws EmployeePayrollDBException {
		EmployeePayrollData employeePayrollData = null;
		Connection connection = null;
		try {
			connection = this.getConnection();
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new EmployeePayrollDBException("Couldn't establish connection.");
		}

		try (Statement statement = connection.createStatement();) {
			String sql = String.format("INSERT INTO company (company_id, company_name) VALUES ('%s', '%s');", companyId,
										companyName);
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into company");
		}

		try (Statement statement = connection.createStatement();) {
			String sql = String.format("INSERT INTO department (department_id, department_name) VALUES ('%s', '%s');",
										departmentId, departmentName);
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into department");
		}

		int employeeId = 0;
		try (Statement statement = connection.createStatement();) {
			String sql = String.format("INSERT INTO employee (name, company_id, gender, address, phone_number, startDate) "
										+ "VALUES ('%s', '%s', '%s', '%s', '%s', '%s');",
										name, companyId, gender, address, phNo, Date.valueOf(startDate));
			int rowsAffected = statement.executeUpdate(sql);
			if (rowsAffected == 1) {
				String sql1 = String.format("SELECT employee_id from employee WHERE name = '%s'", name);
				ResultSet resultSet = statement.executeQuery(sql1);
				if (resultSet.next())
					employeeId = resultSet.getInt("employee_id");
			}
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into employee");
		}

		try (Statement statement = connection.createStatement();) {
			double deductions = salary * 0.2;
			double taxablepay = salary - deductions;
			double tax = taxablepay * 0.1;
			double netpay = salary - tax;
			String sql = String.format("INSERT INTO payroll (employee_id, basic_pay, deductions, taxable_pay, tax, net_pay) "
										+ "VALUES ('%s', '%s', '%s', '%s', '%s', '%s');",
										employeeId, salary, deductions, taxablepay, tax, netpay);
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into payroll");
		}

		try (Statement statement = connection.createStatement();) {
			String sql = String.format("INSERT INTO employee_department (employee_id, department_id) VALUES ('%s', '%s');", 
										employeeId, departmentId);
			int rowsAffected = statement.executeUpdate(sql);
			if (rowsAffected == 1) {
				List<String> deptList = new ArrayList<>();
				deptList.add(departmentName);
				employeePayrollData = new EmployeePayrollData(employeeId, name, salary, startDate, gender, companyName,
																companyId, deptList);
				connection.commit();
			}
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			throw new EmployeePayrollDBException("Unable to insert into employee_department");
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

	
	public EmployeePayrollData deleteEmployee(String name) {
		try (Connection connection = this.getConnection(); 
				Statement statement = connection.createStatement();) {
			String sql = String.format("UPDATE employee SET is_active = false WHERE name = '%s'", name);
			int result = statement.executeUpdate(sql);
			if(result == 1) {
				return this.getEmployeePayrollData(name).get(0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
