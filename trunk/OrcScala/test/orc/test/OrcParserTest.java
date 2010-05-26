//
// OrcParserTest.java -- Java class OrcParserTest
// Project OrcJava
//
// $Id: OrcParserTest.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.error.compiletime.ParsingException;
import orc.OrcParser;

/**
 * This validates the parser simply by trying to parse everything in the
 * "examples" directory.
 *
 * @author quark
 */
public class OrcParserTest {
  public static String readFileAsString(String filePath)
      throws java.io.IOException {
    byte[] buffer = new byte[(int) new File(filePath).length()];
    BufferedInputStream f = new BufferedInputStream(new FileInputStream(
        filePath));
    f.read(buffer);
    return new String(buffer);
  }

  public static Test suite() {
    final TestSuite suite = new TestSuite("orc.test.parser.OrcParserTest");
    final LinkedList<File> files = new LinkedList<File>();
    TestUtils.findOrcFiles(new File("examples"), files);
    for (final File file : files) {
      suite.addTest(new TestCase(file.toString()) {
        @Override
        public void runTest() throws ParsingException, IOException {
          OrcParser.parse(readFileAsString(file.toString()));
        }
      });
    }
    return suite;
  }
}
