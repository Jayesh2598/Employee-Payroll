package com.capg.employeePayrollTest.empPayrollService;

import java.time.LocalDate;
import java.util.Arrays;
import java.sql.Date;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.capg.employeePayroll.model.EmployeePayrollData;
import com.capg.employeePayroll.controller.EmployeePayrollService;
import com.capg.employeePayroll.controller.EmployeePayrollService.IOService;
import com.capg.employeePayroll.jdbc.EmployeePayrollDBException;

import static com.capg.employeePayroll.controller.EmployeePayrollService.IOService.FILE_IO;

public class EmployeePayrollServiceTest {
	@Test
	public void given3EmployeeEntriesWhenWrittenToFileShouldMatchFileEntries() {
		EmployeePayrollData[] empArray = { new EmployeePayrollData(1, "Joe Biden", 10000.0),
											new EmployeePayrollData(2, "Donald Trump", 20000.0),
											new EmployeePayrollData(3, "Michael Bloomberg", 30000.0) };
		EmployeePayrollService employeePayrollService = new EmployeePayrollService(Arrays.asList(empArray));
		employeePayrollService.writeEmployeePayrollData(FILE_IO);
		employeePayrollService.printData(FILE_IO);
		long entries = employeePayrollService.countEntries(FILE_IO);
		System.out.println("Number of entries : " + entries);
		employeePayrollService.readFile(IOService.FILE_IO);
		System.out.println(employeePayrollService.readFile);
		assertTrue(entries == 3);
	}
	
	@Test //JDBC_UC1,2
	public void givenEmployeePayrollDB_WhenRetrieved_ShouldMatchEmployeeCount() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		List<EmployeePayrollData> data = employeePayrollService.readEmployeePayrollDB(IOService.DB_IO);
		assertTrue(data.size() == 3);
	}
	
	@Test //JDBC_UC3,4
	public void givenNewSalaryForEmployee_WhenUpdated_ShouldSyncWithDB() throws EmployeePayrollDBException {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO);
		employeePayrollService.updateEmployeeSalary("Terisa", 3000000.00);
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Terisa");
		assertTrue(result);
	}
	
	@Test //JDBC_UC5
	public void givenDateRange_WhenRetrieved_ShouldMatchEmployeeCount() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO);
		LocalDate startDate = LocalDate.of(2018, 01, 02);
		LocalDate endDate = LocalDate.now();
		List<EmployeePayrollData> data = employeePayrollService.readEmployeePayrollForDateRange(IOService.DB_IO, Date.valueOf(startDate), Date.valueOf(endDate));
		assertTrue(data.size() == 3);
	}
}
