package com.capg.employeePayrollTest.empPayrollService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

import static org.junit.Assert.assertTrue;

import com.capg.employeePayroll.model.EmployeePayrollData;
import com.google.gson.Gson;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import com.capg.employeePayroll.controller.EmployeePayrollService;
import com.capg.employeePayroll.controller.EmployeePayrollService.Database;
import com.capg.employeePayroll.controller.EmployeePayrollService.IOService;
import com.capg.employeePayroll.controller.EmployeePayrollDBService.Operation;
import com.capg.employeePayroll.jdbc.EmployeePayrollDBException;

import static com.capg.employeePayroll.controller.EmployeePayrollService.IOService.FILE_IO;
import static com.capg.employeePayroll.controller.EmployeePayrollService.IOService.REST_IO;

public class EmployeePayrollServiceTest {
	
	private Logger log = Logger.getLogger(EmployeePayrollServiceTest.class.getName());
	
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

	@Test // JDBC_UC1,2
	public void givenEmployeePayrollDB_WhenRetrieved_ShouldMatchEmployeeCount() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		List<EmployeePayrollData> data = employeePayrollService.readEmployeePayrollDB(IOService.DB_IO,
																					Database.DENORMALIZED);
		Assert.assertEquals(3, data.size());

		data = employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.NORMALIZED);
		Assert.assertEquals(4, data.size());
	}

	@Test // JDBC_UC3,4
	public void givenNewSalaryForEmployee_WhenUpdated_ShouldSyncWithDB() throws EmployeePayrollDBException {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.DENORMALIZED);
		employeePayrollService.updateEmployeeSalary("Terisa", 3000000.00, Database.DENORMALIZED);
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Terisa", Database.DENORMALIZED);
		assertTrue(result);

		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.NORMALIZED);
		employeePayrollService.updateEmployeeSalary("Bill", 10000.00, Database.NORMALIZED);
		result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Bill", Database.NORMALIZED);
		assertTrue(result);
	}

	@Test // JDBC_UC5
	public void givenDateRange_WhenRetrieved_ShouldMatchEmployeeCount() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.DENORMALIZED);
		LocalDate startDate1 = LocalDate.of(2018, 01, 02);
		LocalDate endDate1 = LocalDate.now();
		List<EmployeePayrollData> data = employeePayrollService.readEmployeePayrollForDateRange(IOService.DB_IO,
										Database.DENORMALIZED, startDate1, endDate1);
		Assert.assertEquals(3, data.size());

		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.NORMALIZED);
		LocalDate startDate2 = LocalDate.of(2018, 01, 01);
		LocalDate endDate2 = LocalDate.now();
		data = employeePayrollService.readEmployeePayrollForDateRange(IOService.DB_IO, Database.NORMALIZED,
															startDate2, endDate2);
		Assert.assertEquals(4, data.size());
	}

	@Test // JDBC_UC6
	public void givenPayrollData_ShouldReturnProperValue() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.DENORMALIZED);
		Map<String, Double> dataByGender1 = new HashMap<>();
		dataByGender1 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.DENORMALIZED, Operation.SUM);
		assertTrue(dataByGender1.get("M").equals(4000000.00) && dataByGender1.get("F").equals(3000000.00));
		dataByGender1 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.DENORMALIZED, Operation.AVG);
		assertTrue(dataByGender1.get("M").equals(2000000.00) && dataByGender1.get("F").equals(3000000.00));
		dataByGender1 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.DENORMALIZED, Operation.MIN);
		assertTrue(dataByGender1.get("M").equals(1000000.00) && dataByGender1.get("F").equals(3000000.00));
		dataByGender1 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.DENORMALIZED, Operation.MAX);
		assertTrue(dataByGender1.get("M").equals(3000000.00) && dataByGender1.get("F").equals(3000000.00));
		dataByGender1 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.DENORMALIZED, Operation.COUNT);
		assertTrue(dataByGender1.get("M").equals(2.00) && dataByGender1.get("F").equals(1.00));

		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.NORMALIZED);
		Map<String, Double> dataByGender2 = new HashMap<>();
		dataByGender2 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.NORMALIZED, Operation.SUM);
		assertTrue(dataByGender2.get("M").equals(30000.00) && dataByGender2.get("F").equals(70000.00));
		dataByGender2 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.NORMALIZED, Operation.AVG);
		assertTrue(dataByGender2.get("M").equals(15000.00) && dataByGender2.get("F").equals(35000.00));
		dataByGender2 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.NORMALIZED, Operation.MIN);
		assertTrue(dataByGender2.get("M").equals(10000.00) && dataByGender2.get("F").equals(30000.00));
		dataByGender2 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.NORMALIZED, Operation.MAX);
		assertTrue(dataByGender2.get("M").equals(20000.00) && dataByGender2.get("F").equals(40000.00));
		dataByGender2 = employeePayrollService.readDataByGender(IOService.DB_IO, Database.NORMALIZED, Operation.COUNT);
		assertTrue(dataByGender2.get("M").equals(2.00) && dataByGender2.get("F").equals(2.00));
	}

	@Test // JDBC_UC7,8
	public void givenNewEmployee_WhenAdded_ShouldSyncWithDB() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.DENORMALIZED);
		employeePayrollService.addEmployeeToPayroll("Mark", 5000000.00, LocalDate.now(), "M");
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Mark", Database.DENORMALIZED);
		assertTrue(result);

		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.NORMALIZED);
		employeePayrollService.addEmployeeToNormalizedPayroll("Frank", "M", "Paris", "7045279237", 50000,
											LocalDate.of(2019, 11, 25), 5, "HP", "Finance", 105);
		result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Frank", Database.NORMALIZED);
		assertTrue(result);
	}
	
	@Test //JDBC_UC12
	public void givenEmployee_WhenDeleted_ShouldDeleteFromList_AndRemainInDB() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.NORMALIZED);
		boolean result = employeePayrollService.deleteEmployee("Frank");
		assertTrue(result);
	}
	
	@Test //MT_UC1-4
	public void givenEmployees_WhenAddedToDB_ShouldMatchEmployeeEntries() {
		EmployeePayrollData[] arrayOfEmployees = { 
				new EmployeePayrollData(0, "Luffy", 100000.0, LocalDate.now(), "M"),
				new EmployeePayrollData(0, "Deku", 200000.0, LocalDate.now(), "M"),
				new EmployeePayrollData(0, "Naruto", 300000.0, LocalDate.now(), "M"),
				new EmployeePayrollData(0, "Tanjiro", 400000.0, LocalDate.now(), "M"),
				new EmployeePayrollData(0, "Gon", 500000.0, LocalDate.now(), "M"),
				new EmployeePayrollData(0, "Archer", 600000.0, LocalDate.now(), "M") 
				};
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.DENORMALIZED);
		Instant start1 = Instant.now();
		employeePayrollService.addEmployeeToPayroll(Arrays.asList(arrayOfEmployees));
		Instant end1 = Instant.now();
		log.info("Duration without thread : " + Duration.between(start1, end1));
		Instant start2 = Instant.now();
		employeePayrollService.addEmployeeToPayrollWithThreads(Arrays.asList(arrayOfEmployees));
		Instant end2 = Instant.now();
		log.info("Duration with thread : " + Duration.between(start2, end2));
		Assert.assertEquals(13, employeePayrollService.countEntries(IOService.DB_IO));
	}
	
	@Test //MT_UC6
	public void givenNewSalariesForEmployees_WhenUpdated_ShouldSyncWithDB() {
		EmployeePayrollService employeePayrollService = new EmployeePayrollService();
		employeePayrollService.readEmployeePayrollDB(IOService.DB_IO, Database.DENORMALIZED);
		Map<String, Double> empSalaryMap = new HashMap<>();
		empSalaryMap.put("Luffy", 200000.00);
		empSalaryMap.put("Deku", 300000.00);
		empSalaryMap.put("Naruto", 400000.00);
		empSalaryMap.put("Tanjiro", 500000.00);
		empSalaryMap.put("Gon", 600000.00);
		empSalaryMap.put("Archer", 700000.00);
		Instant start = Instant.now();
		employeePayrollService.updateEmployeesSalary(empSalaryMap);
		Instant end = Instant.now();
		log.info("Updating time taken : " + Duration.between(start, end));
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB(empSalaryMap);
		assertTrue(result);
	}
	
	@Before
	public void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = 3000;
	}
	
	public EmployeePayrollData[] getEmployeeList() {
		Response response = RestAssured.get("/employee_payroll");
		System.out.println("Employee_Payroll entries in JSONServer:\n" + response.asString());
		EmployeePayrollData[] arrEmployeePayrollDatas = new Gson().fromJson(response.asString(), EmployeePayrollData[].class);
		return arrEmployeePayrollDatas;
	}
	
	private Response addEmployeeToJsonServer(EmployeePayrollData data) {
		String empJson = new Gson().toJson(data);
		RequestSpecification request = RestAssured.given();
		request.header("Content-Type", "application/json");
		request.body(empJson);
		return request.post("/employee_payroll");
	}
	
	private void addEmployeeToJsonServerWithThreads(List<EmployeePayrollData> list, EmployeePayrollService employeePayrollService) {
		Map<Integer, Boolean> employeeAdditionStatus = new HashMap<>();
		list.forEach(employee -> {
			Runnable task = () -> {
				employeeAdditionStatus.put(employee.hashCode(), false);
				log.log(Level.INFO, ()-> "Employee being added: " + Thread.currentThread().getName());
				Response response = addEmployeeToJsonServer(employee);
				log.log(Level.INFO, ()-> "Employee added: " + Thread.currentThread().getName());
				employeePayrollService.addEmployeeToPayroll(employee, REST_IO);
				employeeAdditionStatus.put(employee.hashCode(), true);
			};
			Thread thread = new Thread(task, employee.name);
			thread.start();
		});
		while(employeeAdditionStatus.containsValue(false)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void givenEmployeeDataInJsonServer_WhenRetrieved_ShouldMatchEmployeeCount() {
		EmployeePayrollData[] arrayOfEmps = getEmployeeList();
		EmployeePayrollService employeePayrollService = new EmployeePayrollService(Arrays.asList(arrayOfEmps));
		long entries = employeePayrollService.countEntries(REST_IO);
		Assert.assertEquals(3, entries);
	}
	
	@Test
	public void givenNewEmployee_WhenAdded_ShouldMatch201ResponseAndCount() {
		EmployeePayrollData[] arrayOfEmps = getEmployeeList();
		EmployeePayrollService employeePayrollService = new EmployeePayrollService(Arrays.asList(arrayOfEmps));
		
		EmployeePayrollData employee = new EmployeePayrollData(4, "Tywin Lannister", 400000, LocalDate.of(2020, 11, 10), "M");
		
		Response response = addEmployeeToJsonServer(employee);
		int statusCode = response.getStatusCode();
		Assert.assertEquals(201, statusCode);
		
		employee = new Gson().fromJson(response.asString(), EmployeePayrollData.class);
		employeePayrollService.addEmployeeToPayroll(employee, REST_IO);
		long entries = employeePayrollService.countEntries(REST_IO);
		Assert.assertEquals(4, entries);
	}
	
	@Test
	public void given3Employees_WhenAdded_ShouldMatchCount() {
		EmployeePayrollData[] arrayOfEmps = getEmployeeList();
		EmployeePayrollService employeePayrollService = new EmployeePayrollService(Arrays.asList(arrayOfEmps));
		EmployeePayrollData[] employeeArray = {
				new EmployeePayrollData(5, "Warren Buffet", 500000, LocalDate.of(2020, 11, 10), "M"),
				new EmployeePayrollData(6, "Larry Page", 600000, LocalDate.of(2020, 11, 10), "M"),
				new EmployeePayrollData(7, "Bernard Arnault", 700000, LocalDate.of(2020, 11, 10), "M"),
		};
		addEmployeeToJsonServerWithThreads(Arrays.asList(employeeArray), employeePayrollService);
		getEmployeeList();
		long entries = employeePayrollService.countEntries(REST_IO);
		Assert.assertEquals(7, entries);
	}
}
