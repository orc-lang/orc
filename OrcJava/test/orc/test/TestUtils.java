package orc.test;

import java.io.File;
import java.util.LinkedList;

public final class TestUtils {
	private TestUtils() {}
	public static void findOrcFiles(File base, LinkedList<File> files) {
		File[] list = base.listFiles();
		if (list == null) return;
		for (File file : list) {
			if (file.isDirectory()) {
				findOrcFiles(file, files);
			} else if (file.getPath().endsWith(".orc")) {
				files.add(file);
			}
		}
	}
}
