//
// OrcParserTest.java -- Java class OrcParserTest
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.compile.StandardOrcCompiler;
import orc.compile.parse.OrcFileInputContext;
import orc.compile.parse.OrcProgramParser;
import orc.error.compiletime.ParsingException;
import orc.script.OrcBindings;
import scala.util.parsing.combinator.Parsers;

/**
 * This validates the parser simply by trying to parse everything in the
 * "test_data" and "../OrcExamples" directory.
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
    TestUtils.findOrcFiles(new File("test_data"), files);
    TestUtils.findOrcFiles(new File("../OrcExamples"), files);
    final OrcBindings options = new OrcBindings();
    final StandardOrcCompiler envServices = new StandardOrcCompiler();
    for (final File file : files) {
      if (file.getAbsolutePath().contains(File.separatorChar+"functional_invalid"+File.separatorChar)) {
        continue; // Skip cases for invalid testing -- TODO:Look for a specific parse failure
      }
      suite.addTest(new TestCase(file.toString()) {
        @Override
        public void runTest() throws ParsingException, IOException {
          OrcFileInputContext ic = new OrcFileInputContext(file, "UTF-8");
          options.filename_$eq(file.toString());
          Parsers.ParseResult<orc.ast.ext.Expression> pr = OrcProgramParser.apply(ic, options, envServices);
          assertTrue("Parsing unsucessful: "+pr.toString(), pr.successful());
        }
      });
    }
    return suite;
  }
}
