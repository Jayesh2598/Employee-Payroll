package com.capg.employeePayroll.model;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class EmployeePayrollData {

	public int id;
	public String name;
	public double salary;
	public LocalDate startDate;
	public String gender;
	private String companyName;
	private int companyId;
	public List<String> departmentList = new ArrayList<>();

	public EmployeePayrollData(int id, String name, double salary) {
		this.id = id;
		this.name = name;
		this.salary = salary;
	}

	public EmployeePayrollData(int id, String name, double salary, LocalDate startDate) {
		this(id, name, salary);
		this.startDate = startDate;
	}
	
	public EmployeePayrollData(int id, String name, double salary, LocalDate startDate, String gender) {
		this(id,name,salary,startDate);
		this.gender = gender;
	}
	
	public EmployeePayrollData(int id, String name, double salary, LocalDate startDate, String gender, String companyName,
			int companyId) {
		this(id, name, salary, startDate,gender);
		this.companyName = companyName;
		this.companyId = companyId;
	}
	
	public EmployeePayrollData(int id, String name, double salary, LocalDate startDate, String gender, String companyName,
			int companyId, List<String> department) {
		this(id, name, salary, startDate, gender, companyName, companyId);
		this.departmentList = department;
	}

	@Override
	public String toString() {
		return "id= " + id + ", name= " + name + ", salary= " + salary;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		EmployeePayrollData that = (EmployeePayrollData) obj;
		return id == that.id && Double.compare(that.salary, salary) == 0 && name.equals(that.name);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, gender, salary, startDate);
	}
}
