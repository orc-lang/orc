//
// OrcParserTest.java -- Java class OrcParserTest
// Project OrcTests
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.proc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import scala.util.parsing.combinator.Parsers;

import orc.compile.CompilerOptions;
import orc.compile.StandardOrcCompiler;
import orc.compile.parse.OrcFileInputContext;
import orc.compile.parse.OrcProgramParser;
import orc.error.compiletime.ExceptionCompileLogger;
import orc.error.compiletime.ParsingException;
import orc.script.OrcBindings;
import orc.test.util.TestUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This validates the parser simply by trying to parse everything in the
 * "test_data" and "../OrcExamples" directory.
 *
 * @author quark
 */
public class OrcParserTest {
    public static String readFileAsString(final String filePath) throws java.io.IOException {
        final byte[] buffer = new byte[(int) new File(filePath).length()];
        final BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath));
        f.read(buffer);
        f.close();
        return new String(buffer);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(OrcParserTest.class.getSimpleName());
        final LinkedList<File> files = new LinkedList<File>();
        TestUtils.findOrcFiles(new File("test_data"), files);
        TestUtils.findOrcFiles(new File("../OrcExamples"), files);
        final OrcBindings options = new OrcBindings();
        final StandardOrcCompiler envServices = new StandardOrcCompiler();
        final CompilerOptions co = new CompilerOptions(options, new ExceptionCompileLogger());
        for (final File file : files) {
            if (file.getAbsolutePath().contains(File.separatorChar + "functional_invalid" + File.separatorChar) && !file.getAbsolutePath().contains(File.separatorChar + "functional_invalid" + File.separatorChar + "parse_fail" + File.separatorChar)) {
                continue; // Only run cases for parser invalid testing
            }
            suite.addTest(new TestCase(file.toString()) {
                @Override
                public void runTest() throws ParsingException, IOException {
                    final OrcFileInputContext ic = new OrcFileInputContext(file, "UTF-8");
                    options.filename_$eq(file.toString());
                    final Parsers.ParseResult<orc.ast.ext.Expression> pr = OrcProgramParser.apply(ic, co, envServices);
                    if (!file.getAbsolutePath().contains(File.separatorChar + "functional_invalid" + File.separatorChar)) {
                        assertTrue("Parsing unsucessful: " + pr.toString(), pr.successful());
                    } else {
                        assertTrue("Parsing did not identify error: " + pr.toString(), !pr.successful());
                    }
                }
            });
        }
        return suite;
    }
}
