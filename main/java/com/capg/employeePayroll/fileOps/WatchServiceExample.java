package com.capg.employeePayroll.fileOps;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class WatchServiceExample {
	
	private static final Kind<?> ENTRY_DELETE = null;
	private static final Kind<?> ENTRY_MODIFY = null;
	private static final Kind<?> ENTRY_CREATE = null;
	private final WatchService watcher;
	private final Map<WatchKey, Path> dirWatchers;
	
	//Create Watch service and register given directory
	public WatchServiceExample(Path path) throws IOException{
		this.watcher = FileSystems.getDefault().newWatchService();
		this.dirWatchers = new HashMap<>();
		scanAndRegisterDirectories(path);
	}

	//Register given directories and all their sub-directories with WatchService
	private void scanAndRegisterDirectories(Path start) throws IOException {
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				registerDirWatchers(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	//Register given directory with watch service
	protected void registerDirWatchers(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		dirWatchers.put(key, dir);
	}
	
	//Process all events for keys queued to the watchers
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void processEvents() {
		while(true) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				return;
			}
			Path dir = dirWatchers.get(key);
			if(dir == null)
				continue;
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				Path name = ((WatchEvent<Path>)event).context();
				Path child = dir.resolve(name);
				System.out.format("%s: %s\n", event.kind().name(), child);
				
				//if directory is created, then register it and its sub-directories
				if(kind == ENTRY_CREATE) {
					try {
						if(Files.isDirectory(child))
							scanAndRegisterDirectories(child);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if (kind.equals(ENTRY_DELETE)) {
					if(Files.isDirectory(child))
						dirWatchers.remove(key);
				}
			}
			
			//reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if(!valid) {
				dirWatchers.remove(key);
				if(dirWatchers.isEmpty())
					break;
			}
		}
	}
}
