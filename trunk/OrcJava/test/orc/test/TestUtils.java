package orc.test;

import java.io.File;
import java.util.LinkedList;

public final class TestUtils {
	private TestUtils() {}
	public static void findOrcFiles(File base, LinkedList<File> files) {
		for (File file : base.listFiles()) {
			if (file.isDirectory()) {
				findOrcFiles(file, files);
			} else if (file.getPath().endsWith(".orc")) {
				files.add(file);
			}
		}
	}
}
