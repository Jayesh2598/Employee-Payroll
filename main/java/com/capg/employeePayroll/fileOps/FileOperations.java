package com.capg.employeePayroll.fileOps;

import java.io.File;

public class FileOperations {

	public static boolean deleteFiles(File filesToDelete) {
		File path[] = filesToDelete.listFiles();
		if(path != null) {
			for(File file : path) {
				deleteFiles(file);
			}
		}
		return filesToDelete.delete();
	}
}
