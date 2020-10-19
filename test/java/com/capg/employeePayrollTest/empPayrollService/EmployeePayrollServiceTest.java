package com.capg.employeePayrollTest.empPayrollService;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.capg.employeePayroll.model.EmployeePayrollData;
import com.capg.employeePayroll.controller.EmployeePayrollService;
import com.capg.employeePayroll.controller.EmployeePayrollService.IOService;

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
}
