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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;

import scala.collection.JavaConverters;
import scala.collection.TraversableOnce;

import orc.test.util.FactorValue;
import orc.test.util.TestEnvironmentDescription;
import orc.util.CsvWriter;
import orc.util.ExecutionLogOutputStream;

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

    public static int countFile(final File file) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(file));
        final int counts = countReader(in);
        in.close();
        return counts;
    }

    public static int repeatRead = Integer.parseInt(System.getProperty("orc.test.repeatRead", "3"));

    public static int repeatCountFilename(final String filename) throws IOException {
        final File f = new File(filename);
        if (!f.canRead()) {
            throw new RuntimeException("Cannot read file: " + f.getCanonicalPath());
        }
        int sum = 0;
        for (int i = 0; i < repeatRead; i++) {
            sum += countFile(f);
        }
        return sum;
    }

    public static String dataDir = "../OrcTests/test_data/functional_valid/distrib/holmes_test_data/";
    public static String[] inputList = { dataDir + "adventure-1.txt", dataDir + "adventure-2.txt", dataDir + "adventure-3.txt", dataDir + "adventure-4.txt", dataDir + "adventure-5.txt", dataDir + "adventure-6.txt", dataDir + "adventure-7.txt", dataDir + "adventure-8.txt", dataDir + "adventure-9.txt", dataDir + "adventure-10.txt", dataDir + "adventure-11.txt", dataDir + "adventure-12.txt" };

    public static int testPayload() throws IOException {
        int sum = 0;
        for (final String fn : inputList) {
            sum += repeatCountFilename(fn);
        }
        return sum;
    }

    public static Long[][] timeRepetitions(final int numRepetitions) throws IOException {
        final Long[][] testElapsedTimes = new Long[numRepetitions][2];
        for (int thisRepetitionNum = 1; thisRepetitionNum <= numRepetitions; thisRepetitionNum++) {
            System.out.println("Repetition " + thisRepetitionNum + ": start.");
            final long startNanos = System.nanoTime();
            final int p = testPayload();
            System.out.println("Repetition " + thisRepetitionNum + ": returned " + p);
            final long finishNanos = System.nanoTime();
            System.out.println("Repetition " + thisRepetitionNum + ": finish.  Elapsed time " + (finishNanos - startNanos) / 1000 + " µs");
            testElapsedTimes[thisRepetitionNum - 1][0] = Long.valueOf(thisRepetitionNum);
            testElapsedTimes[thisRepetitionNum - 1][1] = Long.valueOf((finishNanos - startNanos) / 1000);
        }
        return testElapsedTimes;
    }

    public static void setupOutput() throws IOException {
        if (System.getProperty("orc.executionlog.dir", "").isEmpty()) {
            throw new IllegalArgumentException("java system property orc.executionlog.dir must be set");
        }
        final File outDir = new File(System.getProperty("orc.executionlog.dir"));
        if (!outDir.exists()) {
            throw new IOException("Directory must exist: " + outDir.getAbsolutePath());
        }
        TestEnvironmentDescription.dumpAtShutdown();
    }

    public static void writeCsvFile(final String basename, final String description, final String[] tableColumnTitles, final Object[][] rows) throws IOException {
        try (
            final OutputStream csvOut = ExecutionLogOutputStream.apply(basename, "csv", description).get();
            final OutputStreamWriter csvOsw = new OutputStreamWriter(csvOut, "UTF-8");
        ) {
            
            final ArrayList<TraversableOnce<?>> newRows = new ArrayList<>(rows.length);
            for (final Object[] row : rows) {
                newRows.add(JavaConverters.collectionAsScalaIterable(Arrays.asList(row)));
            }

            final CsvWriter csvWriter = new CsvWriter(csvOsw);
            csvWriter.writeHeader(JavaConverters.collectionAsScalaIterable(Arrays.asList(tableColumnTitles)));
            csvWriter.writeRowsOfTraversables(JavaConverters.collectionAsScalaIterable(newRows));
        }
        System.out.println(description + " written to " + basename + ".csv");
    }

    public static void main(final String[] args) throws IOException {
        TestEnvironmentDescription.dumpAtShutdown();

        final int numRepetitions = Integer.parseInt(System.getProperty("orc.test.numRepetitions", "20"));

        setupOutput();

        final Object[][] factorValues = {
                {"Program", "WordCount.java", "", ""},
                {"Reads per file", Integer.valueOf(repeatRead), "", "Number of sequential re-reads of the file"}
        };
        FactorValue.writeFactorValuesTable(factorValues);

        final Long[][] repetitionTimes = timeRepetitions(numRepetitions);

        final String[] repetitionTitles = { "Repetition number", "Elapsed time (µs)" };
        writeCsvFile("repetition-times", "Repetitions' elapsed times output file", repetitionTitles, repetitionTimes);
    }

}
