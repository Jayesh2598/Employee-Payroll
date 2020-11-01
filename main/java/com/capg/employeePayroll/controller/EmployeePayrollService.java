package com.capg.employeePayroll.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.capg.employeePayroll.fileOps.EmployeePayrollFileIOService;
import com.capg.employeePayroll.model.EmployeePayrollData;

public class EmployeePayrollService {

	public enum IOService {
		CONSOLE_IO, FILE_IO, DB_IO, REST_IO
	}

	private List<EmployeePayrollData> employeePayrollList;
	public List<String> readFile;
	
	public EmployeePayrollService() {	}

	public EmployeePayrollService(List<EmployeePayrollData> employeePayrollList) {
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
			System.out.println("Writing Employee payroll data on Console: " + employeePayrollList);
		else if (ioService.equals(IOService.FILE_IO))
			new EmployeePayrollFileIOService().writeData(employeePayrollList);
	}

	public long countEntries(IOService ioService) {
		if (ioService.equals(IOService.FILE_IO))
			return new EmployeePayrollFileIOService().countEntries();
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

	public List<EmployeePayrollData> readEmployeePayrollDB(IOService ioService) {
		if(ioService == IOService.DB_IO)
			this.employeePayrollList = new EmployeePayrollDBService().readData();
		return employeePayrollList;
	}
}
