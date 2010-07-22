//
// ExamplesTest.java -- Java class ExamplesTest
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import orc.compile.StandardOrcCompiler;
import orc.OrcCompilerProvides;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.CompileLogger;
import orc.error.compiletime.ExceptionCompileLogger;

import test.orc.OrcEngine;

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
 * An output block starting with "OUTPUT:PERMUTABLE" specifies that any permutation
 * of the given values is a valid output. Example:
 * 
 *  <pre>
 * {-
 * OUTPUT:PERMUTABLE
 * 1
 * 2
 * 3
 * -}
 * </pre>
 * says that the program may publish the values 1, 2 and 3 in any order.
 * 
 * If none of the expected outputs match the actual output, the test fails for
 * that example. 
 * 
 * The multiple output blocks and OUTPUT:PERMUTABLE feature let us cope with limited
 * non-determinism, but a better solution is needed for serious testing of
 * non-deterministic programs.
 *
 * @author quark, srosario
 */
public class ExamplesTest {
	public static Test suite() {
		return buildSuite();
	}

	public static TestSuite buildSuite() {
		final TestSuite suite = new TestSuite("orc.test.ExamplesTest");
		final LinkedList<File> files = new LinkedList<File>();
		TestUtils.findOrcFiles(new File("examples"), files);
		for (final File file : files) {
			final ExpectedOutput expecteds;
			try {
				expecteds = extractExpectedOutput(file);
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
			// skip tests with no expected output
			if (expecteds.isEmpty()) {
				continue;
			}
			suite.addTest(new TestCase(file.toString()) {
				@Override
				public void runTest() throws IOException, CompilationException, InterruptedException, Throwable, TimeoutException {
				    System.out.println("\n==== Starting "+file+" ====");
					runOrcProgram(file, expecteds);
				}
			});
		}
		return suite;
	}

    private static class ExamplesOptions implements orc.OrcOptions {
        protected ExamplesOptions() { }

        public String filename() { return ""; }
  
        public void filename_$eq(String newVal) { throw new UnsupportedOperationException(); }
  
        public int debugLevel() { return 0; }
  
        public void debugLevel_$eq(int newVal) { throw new UnsupportedOperationException(); }
  
        public boolean usePrelude() { return true; }
  
        public void usePrelude_$eq(boolean newVal) { throw new UnsupportedOperationException(); }
  
        public List<String> includePath() { List<String> r = new ArrayList<String>(1); r.add("."); return r; }
        
        public void includePath_$eq(List<String> newVal) { throw new UnsupportedOperationException(); }
  
        public List<String> additionalIncludes() { return new ArrayList<String>(0); }
        
        public void additionalIncludes_$eq(List<String> newVal) { throw new UnsupportedOperationException(); }
  
        public boolean exceptionsOn() { return false; }
  
        public void exceptionsOn_$eq(boolean newVal) { throw new UnsupportedOperationException(); }
  
        public boolean typecheck() { return false; }
  
        public void typecheck_$eq(boolean newVal) { throw new UnsupportedOperationException(); }
  
        public int maxPublications() { return -1; }
  
        public void maxPublications_$eq(int newVal) { throw new UnsupportedOperationException(); }
  
        public int tokenPoolSize() { return -1; }
  
        public void tokenPoolSize_$eq(int newVal) { throw new UnsupportedOperationException(); }
  
        public int stackSize() { return -1; }
  
        public void stackSize_$eq(int newVal) { throw new UnsupportedOperationException(); }
  
        public List<String> classPath() { return new ArrayList<String>(0); }
  
        public void classPath_$eq(List<String> newVal) { throw new UnsupportedOperationException(); }
  
        public boolean hasCapability(String capName) { throw new UnsupportedOperationException(); }
  
        public void setCapability(String capName, boolean newVal) { throw new UnsupportedOperationException(); }
    }
  	private static ExamplesOptions examplesOptions = new ExamplesOptions(); 
 
	public static void runOrcProgram(final File file, final ExpectedOutput expecteds) throws InterruptedException, Throwable, CompilationException, IOException, TimeoutException {

	    OrcCompilerProvides compiler = new StandardOrcCompiler() {
          private final CompileLogger compileLoggerRef = new ExceptionCompileLogger();
	      @Override public CompileLogger compileLogger() { return compileLoggerRef; }
	    };
		final orc.oil.nameless.Expression expr = compiler.apply(new FileReader(file), examplesOptions);

		if (expr == null) {
			throw new CompilationException("Compilation to OIL failed");
		}
		final OrcEngine engine = new OrcEngine();

		// run the engine with a fixed timeout
		final FutureTask<?> future = new FutureTask<Void>(new Runnable() {
			public void run() {
				engine.run(expr, examplesOptions);
			}
		}, null);
		new Thread(future).start();
		try {
			future.get(10L, TimeUnit.SECONDS);
		} catch (final TimeoutException e) {
			future.cancel(true);
			throw e;
		} catch (final ExecutionException e) {
			throw e.getCause();
		} finally { 
		  engine.stop(); 
		}
		

		// compare the output to the expected result
		final String actual = engine.getOut().toString();
		if (expecteds.contains(actual)) {
		  return;
		}
		throw new AssertionError("Unexpected output:\n" + actual);
	}

	private static ExpectedOutput extractExpectedOutput(final File file) throws IOException {
	  final BufferedReader r = new BufferedReader(new FileReader(file));
	  List<MaybePermutableOutput> outputs = new LinkedList<MaybePermutableOutput>();
	  
	  boolean permutable = false;
	  StringBuilder oneOutput = null;
	  for (String line = r.readLine(); line != null; line = r.readLine()) {
	      if (oneOutput != null) {
	          if (line.startsWith("-}")) {
	              outputs.add(new MaybePermutableOutput(permutable,oneOutput.toString()));
	              oneOutput = null;
	          } else {
	              oneOutput.append(line);
	              oneOutput.append("\n");
	          }
	      } else if (line.startsWith("OUTPUT:PERMUTABLE")) {
	        permutable = true;
	        oneOutput = new StringBuilder();
	      } else if (line.startsWith("OUTPUT:")) {
	        permutable = false;
	        oneOutput = new StringBuilder();
	      }
	  }
	  return new ExpectedOutput(outputs);
	}
}

class ExpectedOutput {
  private List<MaybePermutableOutput> outs = new LinkedList<MaybePermutableOutput>();
  
  public ExpectedOutput(List<MaybePermutableOutput> outputs) {
    this.outs = outputs;
  }
  
  public boolean contains(String actual) {
    for(MaybePermutableOutput o : outs) {
      if(o.matches(actual))
        return true;
    }
    return false;
  }
  
  public boolean isEmpty() {
    return outs.isEmpty();
  }
}

class MaybePermutableOutput {
  private boolean permutable = false;
  String output;
  
  public MaybePermutableOutput(boolean perm, String out) {
    this.permutable = perm;
    this.output = out;
  }

  public boolean matches(String actual) {
      if(!permutable)
        return output.equals(actual);
      
      // Check all if the actual output is a permutation of the expected output.
      String[] actualArr = actual.split("\\n");
      String[] expectedArr = output.split("\\n");
      
      if(actualArr.length != expectedArr.length)
        return false;
      
      LinkedList<String> actuals = new LinkedList<String>();
      for(String s : actualArr) {
        actuals.add(s);
      }
      
      LinkedList<String> expected = new LinkedList<String>();
      for(String s : expectedArr) {
        expected.add(s);
      }
      
      for(String s: actuals) {
        if(!expected.contains(s))
          return false;
        
        expected.remove(s);
      }

      return true;
    }
}