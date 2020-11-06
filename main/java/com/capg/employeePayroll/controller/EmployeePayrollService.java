package com.capg.employeePayroll.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.capg.employeePayroll.controller.EmployeePayrollDBService.Operation;
import com.capg.employeePayroll.fileOps.EmployeePayrollFileIOService;
import com.capg.employeePayroll.jdbc.EmployeePayrollDBException;
import com.capg.employeePayroll.model.EmployeePayrollData;

public class EmployeePayrollService {

	public enum IOService {
		CONSOLE_IO, FILE_IO, DB_IO, REST_IO
	}

	public enum Database {
		DENORMALIZED, NORMALIZED;
	}

	private static Logger log = Logger.getLogger(EmployeePayrollService.class.getName());
	private List<EmployeePayrollData> employeePayrollList;
	private List<String> readFile;
	private EmployeePayrollDBService employeePayrollDBService;
	private EmployeePayrollNormalizedDBService employeePayrollNormalizedDBService;

	public List<String> getReadFile() {
		return readFile;
	}

	public EmployeePayrollService() {
		employeePayrollDBService = EmployeePayrollDBService.getInstance();
		employeePayrollNormalizedDBService = EmployeePayrollNormalizedDBService.getInstance();
	}

	public EmployeePayrollService(List<EmployeePayrollData> employeePayrollList) {
		this();
		this.employeePayrollList = employeePayrollList;
	}

	public static void main(String[] args) {
		ArrayList<EmployeePayrollData> employeePayrollList = new ArrayList<>();
		EmployeePayrollService employeePayrollService = new EmployeePayrollService(employeePayrollList);
		Scanner consoleInputReader = new Scanner(System.in);
		employeePayrollService.readEmployeePayrollData(consoleInputReader);
		employeePayrollService.writeEmployeePayrollData(IOService.CONSOLE_IO);
		employeePayrollService.writeEmployeePayrollData(IOService.FILE_IO);
		System.out.println("Number of entries : " + employeePayrollService.countEntries(IOService.FILE_IO));
		employeePayrollService.readFile(IOService.FILE_IO);
		System.out.println(employeePayrollService.readFile);
	}

	private void readEmployeePayrollData(Scanner consoleInputReader) {
		System.out.println("Enter the employee ID : ");
		int id = Integer.parseInt(consoleInputReader.nextLine());
		System.out.println("Enter the employee name : ");
		String name = consoleInputReader.nextLine();
		System.out.println("Enter the employee's salary : ");
		double salary = Double.parseDouble(consoleInputReader.nextLine());
		employeePayrollList.add(new EmployeePayrollData(id, name, salary));
	}

	public void writeEmployeePayrollData(IOService ioService) {
		if (ioService.equals(IOService.CONSOLE_IO))
			log.log(Level.INFO, () -> "Writing Employee payroll data on Console: " + employeePayrollList);
		else if (ioService.equals(IOService.FILE_IO))
			new EmployeePayrollFileIOService().writeData(employeePayrollList);
	}

	public long countEntries(IOService ioService) {
		if (ioService.equals(IOService.FILE_IO))
			return new EmployeePayrollFileIOService().countEntries();
		if (ioService.equals(IOService.DB_IO))
			return employeePayrollList.size();
		return 0;
	}

	public void printData(IOService ioService) {
		if (ioService.equals(IOService.FILE_IO))
			new EmployeePayrollFileIOService().printData();

	}

	public void readFile(IOService ioService) {
		if (ioService.equals(IOService.FILE_IO))
			this.readFile = new EmployeePayrollFileIOService().readFile();
	}

	public List<EmployeePayrollData> readEmployeePayrollDB(IOService ioService, Database dbService) {
		if (ioService == IOService.DB_IO) {
			if (dbService == Database.DENORMALIZED)
				this.employeePayrollList = employeePayrollDBService.readData();
			if (dbService == Database.NORMALIZED)
				this.employeePayrollList = employeePayrollNormalizedDBService.readData();
		}
		return employeePayrollList;
	}

	public void updateEmployeeSalary(String name, double salary, Database dbService) throws EmployeePayrollDBException {
		int result = 0;
		if (dbService == Database.DENORMALIZED)
			result = employeePayrollDBService.updateEmployeeData(name, salary);
		if (dbService == Database.NORMALIZED)
			result = employeePayrollNormalizedDBService.updateEmployeeData(name, salary);
		if (result == 0)
			throw new EmployeePayrollDBException("No updation performed.");
		EmployeePayrollData employeePayrollData = this.getEmployeePayrollData(name);
		if (employeePayrollData != null)
			employeePayrollData.salary = salary;
	}

	private EmployeePayrollData getEmployeePayrollData(String name) {
		return this.employeePayrollList.stream()
					.filter(item -> item.name.equals(name))
					.findFirst()
					.orElse(null);
	}

	public boolean checkEmployeePayrollInSyncWithDB(String name, Database dbService) {
		if (dbService == Database.DENORMALIZED) {
			List<EmployeePayrollData> employeePayrollDataList = employeePayrollDBService.getEmployeePayrollData(name);
			return employeePayrollDataList.get(0).equals(getEmployeePayrollData(name));
		} else {
			List<EmployeePayrollData> employeePayrollDataList = employeePayrollNormalizedDBService
					.getEmployeePayrollData(name);
			return employeePayrollDataList.get(0).equals(getEmployeePayrollData(name));
		}
	}

	public List<EmployeePayrollData> readEmployeePayrollForDateRange(IOService ioService, Database dbService,
																		Date startDate, Date endDate) {
		if (ioService == IOService.DB_IO) {
			if (dbService == Database.DENORMALIZED)
				return employeePayrollDBService.getEmployeePayrollForDateRange(startDate, endDate);
			if (dbService == Database.NORMALIZED)
				return employeePayrollNormalizedDBService.getEmployeePayrollForDateRange(startDate, endDate);
		}
		return null;
	}

	public Map<String, Double> readDataByGender(IOService ioService, Database dbService, Operation operation) {
		if (ioService == IOService.DB_IO) {
			if (dbService == Database.DENORMALIZED)
				return employeePayrollDBService.getDataByGender(operation);
			if (dbService == Database.NORMALIZED)
				return employeePayrollNormalizedDBService.getDataByGender(operation);
		}
		return null;
	}

	public void addEmployeeToPayroll(String name, double salary, Date startDate, String gender) {
		try {
			employeePayrollList.add(employeePayrollDBService.addEmployeeToPayroll(name, salary, startDate, gender));
		} catch (EmployeePayrollDBException e) {
			log.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public void addEmployeeToPayroll(List<EmployeePayrollData> list) {
		list.forEach(employee -> this.addEmployeeToPayroll(employee.name, employee.salary, employee.startDate, employee.gender));
	}

	public void addEmployeeToNormalizedPayroll(String name, String gender, String address, String phNo, double salary,
							Date startDate, int companyId, String companyName, String departmentName, int departmentId) {
		try {
			employeePayrollList.add(employeePayrollNormalizedDBService.addEmployeeToPayroll(name, gender, address, phNo,
												salary, startDate, companyId, companyName, departmentName, departmentId));
		} catch (EmployeePayrollDBException e) {
			log.log(Level.SEVERE, e.getMessage());
		}
	}

	public boolean deleteEmployee(String name) {
		return employeePayrollList.remove(employeePayrollNormalizedDBService.deleteEmployee(name));
	}
}
