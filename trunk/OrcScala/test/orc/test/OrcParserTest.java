//
// OrcParserTest.java -- Java class OrcParserTest
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.error.compiletime.ParsingException;
import orc.compile.parse.OrcParser;
import scala.collection.immutable.PagedSeq;
import scala.util.parsing.combinator.Parsers;
import scala.util.parsing.input.StreamReader;

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
        	Parsers.ParseResult<orc.compile.ext.Expression> pr = OrcParser.parse(parserOptions, new StreamReader(PagedSeq.fromReader(new FileReader(file)), 0, 1));
        	assertTrue("Parsing unsucessful: "+pr.toString(), pr.successful());
        }
      });
    }
    return suite;
  }

  private static class ParserOptions implements orc.OrcOptions {
    public String filename() { return ""; }

    public void filename_$eq(String newVal) { throw new UnsupportedOperationException(); }

    public int debugLevel() { return 0; }

    public void debugLevel_$eq(int newVal) { throw new UnsupportedOperationException(); }

    public boolean shortErrors() { return false; }

    public void shortErrors_$eq(boolean newVal) { throw new UnsupportedOperationException(); }

    public boolean noPrelude() { return false; }

    public void noPrelude_$eq(boolean newVal) { throw new UnsupportedOperationException(); }

    public scala.collection.immutable.List<String> includePath() { throw new UnsupportedOperationException(); }

    public void includePath_$eq(scala.collection.immutable.List<String> newVal) { throw new UnsupportedOperationException(); }

    public boolean exceptionsOn() { return false; }

    public void exceptionsOn_$eq(boolean newVal) { throw new UnsupportedOperationException(); }

    public boolean typecheck() { return false; }

    public void typecheck_$eq(boolean newVal) { throw new UnsupportedOperationException(); }

    public boolean quietChecking() { return false; }

    public void quietChecking_$eq(boolean newVal) { throw new UnsupportedOperationException(); }

    public int maxPublications() { return -1; }

    public void maxPublications_$eq(int newVal) { throw new UnsupportedOperationException(); }

    public int tokenPoolSize() { return -1; }

    public void tokenPoolSize_$eq(int newVal) { throw new UnsupportedOperationException(); }

    public int stackSize() { return -1; }

    public void stackSize_$eq(int newVal) { throw new UnsupportedOperationException(); }

    public scala.collection.immutable.List<String> classPath() { throw new UnsupportedOperationException(); }

    public void classPath_$eq(scala.collection.immutable.List<String> newVal) { throw new UnsupportedOperationException(); }

    public boolean hasCapability(String capName) { throw new UnsupportedOperationException(); }

    public void setCapability(String capName, boolean newVal) { throw new UnsupportedOperationException(); }
  }
  private static ParserOptions parserOptions = new ParserOptions(); 
}
