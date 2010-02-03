//
// TestUtils.java -- Java class TestUtils
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.util.LinkedList;

public final class TestUtils {
	private TestUtils() {
	}

	public static void findOrcFiles(final File base, final LinkedList<File> files) {
		final File[] list = base.listFiles();
		if (list == null) {
			return;
		}
		for (final File file : list) {
			if (file.isDirectory()) {
				findOrcFiles(file, files);
			} else if (file.getPath().endsWith(".orc")) {
				files.add(file);
			}
		}
	}
}
