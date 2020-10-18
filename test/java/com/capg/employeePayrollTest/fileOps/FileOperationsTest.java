package com.capg.employeePayrollTest.fileOps;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import org.junit.Test;

import com.capg.employeePayroll.fileOps.FileOperations;

public class FileOperationsTest {

	private String HOME = System.getProperty("user.home");
	private String NIO_TEMP_DIR = "TempDirectory";

	@Test
	public void givenFilePathWhenCheckedThenConfirm() throws IOException {
		// Checking File existence
		Path homePath = Paths.get(HOME);
		assertTrue(Files.exists(homePath));

		// Delete File and Check
		Path playPath = Paths.get(HOME + "/" + NIO_TEMP_DIR);
		if (Files.exists(playPath))
			FileOperations.deleteFiles(playPath.toFile());

		// Creating directory
		Files.createDirectory(playPath);
		assertTrue(Files.exists(playPath));

		// Creating file
		IntStream.range(1, 10).forEach(cntr -> {
			Path tempFile = Paths.get(playPath + "/temp" + cntr);
			assertTrue(Files.notExists(tempFile));
			try {
				Files.createFile(tempFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			assertTrue(Files.exists(tempFile));
		});

		// Lising files
		Files.list(playPath).filter(Files::isRegularFile).forEach(System.out::println);
		Files.newDirectoryStream(playPath).forEach(System.out::println);
		Files.newDirectoryStream(playPath, path -> path.toFile().isFile() && path.toString().contains("temp"))
				.forEach(System.out::println);
	}

}
