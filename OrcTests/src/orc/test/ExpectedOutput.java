//
// ExpectedOutput.java -- Java class ExpectedOutput
// Project OrcTests
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
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
import java.util.LinkedList;
import java.util.List;

/**
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
 * An output block starting with "OUTPUT:PERMUTABLE" specifies that any
 * permutation of the given values is a valid output. Example:
 *
 * <pre>
 * {-
 * OUTPUT:PERMUTABLE
 * 1
 * 2
 * 3
 * -}
 * </pre>
 *
 * says that the program may publish the values 1, 2 and 3 in any order. If none
 * of the expected outputs match the actual output, the test fails for that
 * example. The multiple output blocks and OUTPUT:PERMUTABLE feature let us cope
 * with limited non-determinism, but a better solution is needed for serious
 * testing of non-deterministic programs.
 *
 * @author quark, srosario
 */
public class ExpectedOutput {
    private final List<MaybePermutableOutput> outs = new LinkedList<MaybePermutableOutput>();
    private boolean shouldBenchmark = false;

    public ExpectedOutput(final File file) throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(file));

        boolean permutable = false;
        StringBuilder oneOutput = null;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            if (oneOutput != null) {
                if (line.startsWith("-}")) {
                    outs.add(new MaybePermutableOutput(permutable, oneOutput.toString()));
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
            } else if (line.startsWith("BENCHMARK")) {
              shouldBenchmark = true;
            }
        }
    }

    public boolean contains(final String actual) {
        for (final MaybePermutableOutput o : outs) {
            if (o.matches(actual)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return outs.isEmpty();
    }

    public boolean shouldBenchmark() {
      return shouldBenchmark;
    }

    static class MaybePermutableOutput {
        private boolean permutable = false;
        String output;

        public MaybePermutableOutput(final boolean perm, final String out) {
            this.permutable = perm;
            this.output = out;
        }

        public boolean matches(final String actual) {
            if (!permutable) {
                return output.equals(actual);
            }

            // Check all if the actual output is a permutation of the expected
            // output.
            final String[] actualArr = actual.split("\\n");
            final String[] expectedArr = output.split("\\n");

            if (actualArr.length != expectedArr.length) {
                return false;
            }

            final LinkedList<String> actuals = new LinkedList<String>();
            for (final String s : actualArr) {
                actuals.add(s);
            }

            final LinkedList<String> expected = new LinkedList<String>();
            for (final String s : expectedArr) {
                expected.add(s);
            }

            for (final String s : actuals) {
                if (!expected.contains(s)) {
                    return false;
                }
                expected.remove(s);
            }

            return true;
        }
    }
}
