package com.capg.employeePayroll.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
		this.employeePayrollList = new ArrayList<>(employeePayrollList);
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
		else
			return employeePayrollList.size();
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
	
	public void updateEmployeeSalary(String name, double salary) {
		EmployeePayrollData employeePayrollData = this.getEmployeePayrollData(name);
		if (employeePayrollData != null)
			employeePayrollData.salary = salary;
	}
	
	public void updateEmployeesSalary(Map<String, Double> empSalaryMap) {
		Map<Integer, Boolean> employeeSalaryUpdationStatus = new HashMap<>();
		empSalaryMap.forEach((employeeName, salary) -> {
			Runnable task = () -> {
				employeeSalaryUpdationStatus.put(employeeName.hashCode(), false);
				log.log(Level.INFO, ()-> "Employee being updated: " + Thread.currentThread().getName());
				try {
					this.updateEmployeesSalary(employeeName, salary);
				} catch (EmployeePayrollDBException e) {
					System.out.println(e.getMessage());
				}
				employeeSalaryUpdationStatus.put(employeeName.hashCode(), true);
				log.log(Level.INFO, ()-> "Employee updated: " + Thread.currentThread().getName());
			};
			Thread thread = new Thread(task, employeeName);
			thread.start();
		});
		while(employeeSalaryUpdationStatus.containsValue(false)) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateEmployeesSalary(String employeeName, Double salary) throws EmployeePayrollDBException {
		int result = 0;
		result = employeePayrollDBService.updateEmployeeSalaryInBothTables(employeeName, salary);
		if (result == 0)
			throw new EmployeePayrollDBException("No updation performed.");
		EmployeePayrollData employeePayrollData = this.getEmployeePayrollData(employeeName);
		if (employeePayrollData != null)
			employeePayrollData.salary = salary;
	}

	public EmployeePayrollData getEmployeePayrollData(String name) {
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
	
	public boolean checkEmployeePayrollInSyncWithDB(Map<String, Double> empSalaryMap) {
		List<EmployeePayrollData> employeePayrollDataList = new ArrayList<>();
		empSalaryMap.forEach((employeeName, salary) -> {
						EmployeePayrollData obj = employeePayrollDBService.getEmployeePayrollData(employeeName).get(0);
						employeePayrollDataList.add(obj);
					});
		boolean result = true;
		for(EmployeePayrollData data: employeePayrollDataList) {
			boolean b = employeePayrollList.contains(data);
			result = result && b;
		}
		return result;
	}

	public List<EmployeePayrollData> readEmployeePayrollForDateRange(IOService ioService, Database dbService,
																		LocalDate startDate, LocalDate endDate) {
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
	
	public void addEmployeeToPayroll(EmployeePayrollData employee, IOService ioService) {
		if(ioService.equals(IOService.DB_IO))
			this.addEmployeeToPayroll(employee.name, employee.salary, employee.startDate, employee.gender);
		else employeePayrollList.add(employee);
	}

	public void addEmployeeToPayroll(String name, double salary, LocalDate startDate, String gender) {
		try {
			employeePayrollList.add(employeePayrollDBService.addEmployeeToPayroll(name, salary, startDate, gender));
		} catch (EmployeePayrollDBException e) {
			log.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public void addEmployeeToPayroll(List<EmployeePayrollData> list) {
		list.forEach(employee -> this.addEmployeeToPayroll(employee.name, employee.salary, employee.startDate, employee.gender));
	}
	
	public void addEmployeeToPayrollWithThreads(List<EmployeePayrollData> list) {
		Map<Integer, Boolean> employeeAdditionStatus = new HashMap<>();
		list.forEach(employee -> {
			Runnable task = () -> {
				employeeAdditionStatus.put(employee.hashCode(), false);
				log.log(Level.INFO, ()-> "Employee being added: " + Thread.currentThread().getName());
				this.addEmployeeToPayroll(employee.name, employee.salary, employee.startDate, employee.gender);
				employeeAdditionStatus.put(employee.hashCode(), true);
				log.log(Level.INFO, ()-> "Employee added: " + Thread.currentThread().getName());
			};
			Thread thread = new Thread(task, employee.name);
			thread.start();
		});
		while(employeeAdditionStatus.containsValue(false)) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void addEmployeeToNormalizedPayroll(String name, String gender, String address, String phNo, double salary,
							LocalDate startDate, int companyId, String companyName, String departmentName, int departmentId) {
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
