package orc.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kohsuke.args4j.CmdLineException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.Config;
import orc.Orc;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.ParsingException;
import orc.parser.OrcParser;
import orc.runtime.OrcEngine;

/**
 * Test Orc by running annotated sample programs from the "examples" directory.
 * Each program is given at most 10 seconds to complete.
 * 
 * <p>
 * We look for one or more comment blocks with a line starting with "OUTPUT:",
 * and take everything in the comment after the "OUTPUT:" line to be a possible
 * output of the program. Example:
 * 
 * <pre>
 * {-
 * OUTPUT:
 * 1
 * 2
 * -}
 * 1 | 2
 * </pre>
 * 
 * If none of the expected outputs match the actual output, the test fails for
 * that example. The multiple output blocks let us cope with limited
 * non-determinism, but a better solution is needed for serious testing of
 * non-deterministic programs.
 * 
 * @author quark
 */
public class ExamplesTest {
	public static Test suite() {
		TestSuite suite = new TestSuite("orc.test.ExamplesTest");
		LinkedList<File> files = new LinkedList<File>();
		TestUtils.findOrcFiles(new File("examples"), files);
		for (final File file : files) {
			final LinkedList<String> expecteds;
			try {
				expecteds = extractExpectedOutput(file);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			// skip tests with no expected output
			if (expecteds.isEmpty()) continue;
			suite.addTest(new TestCase(file.toString()) {
				@Override
				public void runTest() throws IOException, CmdLineException, CompilationException, InterruptedException, ExecutionException, TimeoutException {
					runOrcProgram(file, expecteds);
				}
			});
		}
		return suite;
	}
	
	public static void runOrcProgram(File file, LinkedList<String> expecteds)
	throws InterruptedException, ExecutionException, CmdLineException,
			CompilationException, IOException, TimeoutException
	{
		// configure engine to write to a ByteArray
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Config config = new Config();
		config.setInputFile(file);
		config.setStdout(new PrintStream(out));
		config.setStderr(config.getStdout());
		OrcEngine engine = new OrcEngine(config);
		engine.start(Orc.compile(config));
		
		// run the engine with a fixed timeout
		FutureTask<?> future = new FutureTask<Void>(engine, null);
		new Thread(future).start();
		try {
			future.get(10L, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw e;
		}
		
		// compare the output to the expected result
		String actual = out.toString();
		for (String expected : expecteds) {
			if (expected.equals(actual)) return;
		}
		throw new AssertionError("Unexpected output:\n" + actual);
	}
	
	public static LinkedList<String> extractExpectedOutput(File file) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(file));
		LinkedList<String> out = new LinkedList<String>();
		StringBuilder oneOutput = null;
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			if (oneOutput != null) {
				if (line.startsWith("-}")) {
					out.add(oneOutput.toString());
					oneOutput = null;
				} else {
					oneOutput.append(line);
					oneOutput.append("\n");
				}
			} else if (line.startsWith("OUTPUT:")) {
				oneOutput = new StringBuilder();
			}
		}
		return out;
	}
}
