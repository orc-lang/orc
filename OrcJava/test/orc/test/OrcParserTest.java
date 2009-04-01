package orc.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.Config;
import orc.error.compiletime.ParsingException;
import orc.parser.OrcParser;

/**
 * This validates the parser simply by trying to parse
 * everything in the "examples" directory.
 * 
 * @author quark
 */
public class OrcParserTest {
   public static Test suite() {
		TestSuite suite = new TestSuite("orc.test.parser.OrcParserTest");
		LinkedList<File> files = new LinkedList<File>();
		TestUtils.findOrcFiles(new File("examples"), files);
		for (final File file : files) {
			suite.addTest(new TestCase(file.toString()) {
				@Override
				public void runTest() throws ParsingException, IOException {
					OrcParser parser = new OrcParser(new Config(), new FileReader(file));
					parser.parseProgram();
				}
			});
		}
		return suite;
	}
}
