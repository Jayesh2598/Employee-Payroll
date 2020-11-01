package com.capg.employeePayroll.model;

import java.util.Date;

public class EmployeePayrollData {

	public int id;
	public String name;
	public double salary;
	public Date startDate;

	public EmployeePayrollData(Integer id, String name, Double salary) {
		this.id = id;
		this.name = name;
		this.salary = salary;
	}

	public EmployeePayrollData(Integer id, String name, Double salary, Date startDate) {
		this(id, name, salary);
		this.startDate = startDate;
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
}
