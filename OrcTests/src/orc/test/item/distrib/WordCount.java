//
// WordCount.java -- Java class WordCount
// Project OrcTests
//
// Created by jthywiss on Sep 17, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.BreakIterator;

/**
 * @author jthywiss
 */
public class WordCount {

    /**
     * Static members only, no instances
     */
    private WordCount() {
    }

    public static boolean containsAlphabetic(final String s, final int startPos, final int endPos) {
        for (int currPos = startPos; currPos < endPos; currPos++) {
            if (Character.isAlphabetic(s.codePointAt(currPos))) {
                return true;
            }
        }
        return false;
    }

    public static int countLine(final String line) {
        final BreakIterator wb = BreakIterator.getWordInstance();
        wb.setText(line);
        int startPos = 0;
        int endPos = wb.next();
        int words = 0;
        while (endPos >= 0) {
            if (containsAlphabetic(line, startPos, endPos)) {
                words += 1;
            }
            startPos = endPos;
            endPos = wb.next();
        }
        return words;
    }

    public static int countReader(final BufferedReader in) throws IOException {
        int words = 0;
        for (;;) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            words += countLine(line);
        }
        return words;
    }

}
