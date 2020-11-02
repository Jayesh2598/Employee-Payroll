package com.capg.employeePayrollTest.empPayrollService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.capg.employeePayroll.model.EmployeePayrollData;

import com.capg.employeePayroll.controller.EmployeePayrollService;
import com.capg.employeePayroll.controller.EmployeePayrollService.IOService;
import com.capg.employeePayroll.controller.EmployeePayrollDBService.Operation;
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
		System.out.println(employeePayrollService.getReadFile());
		Assert.assertEquals(3, entries);
	}
	
	@Test //JDBC_UC1,2
	public void givenEmployeePayrollDB_WhenRetrieved_ShouldMatchEmployeeCount() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		List<EmployeePayrollData> data = employeePayrollService.readEmployeePayrollDB(IOService.DB_IO);
		assertEquals(3, data.size());
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
		Assert.assertEquals(3, data.size());
	}
	
	@Test //JDBC_UC6
	public void givenPayrollData_ShouldReturnProperValue() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO);
		Map<String, Double> dataByGender = new HashMap<>();
		dataByGender = employeePayrollService.readDataByGender(IOService.DB_IO, Operation.SUM);
		assertTrue(dataByGender.get("M").equals(4000000.00) && dataByGender.get("F").equals(3000000.00));
		dataByGender = employeePayrollService.readDataByGender(IOService.DB_IO, Operation.AVG);
		assertTrue(dataByGender.get("M").equals(2000000.00) && dataByGender.get("F").equals(3000000.00));
		dataByGender = employeePayrollService.readDataByGender(IOService.DB_IO, Operation.MIN);
		assertTrue(dataByGender.get("M").equals(1000000.00) && dataByGender.get("F").equals(3000000.00));
		dataByGender = employeePayrollService.readDataByGender(IOService.DB_IO, Operation.MAX);
		assertTrue(dataByGender.get("M").equals(3000000.00) && dataByGender.get("F").equals(3000000.00));
		dataByGender = employeePayrollService.readDataByGender(IOService.DB_IO, Operation.COUNT);
		assertTrue(dataByGender.get("M").equals(2.00) && dataByGender.get("F").equals(1.00));
	}
	
	@Test //JDBC_UC7
	public void givenNewEmployee_WhenAdded_ShouldSyncWithDB() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO);
		employeePayrollService.addEmployeeToPayroll("Mark", 5000000.00, Date.valueOf(LocalDate.now()), "M");
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Mark");
		assertTrue(result);
	}
}
