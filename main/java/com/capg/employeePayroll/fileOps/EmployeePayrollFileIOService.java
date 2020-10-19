package com.capg.employeePayroll.fileOps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.capg.employeePayroll.model.EmployeePayrollData;

public class EmployeePayrollFileIOService {

	public static String PAYROLL_FILE = "payroll-file.txt";

	public void writeData(List<EmployeePayrollData> employeePayrollList) {
		StringBuffer empBuffer = new StringBuffer();
		employeePayrollList.forEach(employee -> {
			String empString = employee.toString().concat("\n");
			empBuffer.append(empString);
		});

		try {
			Files.write(Paths.get(PAYROLL_FILE), empBuffer.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printData() {
		try {
			Files.lines(new File(PAYROLL_FILE).toPath()).forEach(System.out::println);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long countEntries() {
		long entries = 0;
		try {
			entries = Files.lines(new File(PAYROLL_FILE).toPath()).count();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return entries;
	}
	
	public List<String> readFile() {
		List<String> fileEntries = new ArrayList<>();
		try {
			fileEntries = Files.lines(new File(PAYROLL_FILE).toPath()).map(line -> line.trim()).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileEntries;
	}
}