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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // Lines: 5
    public static boolean containsAlphabetic(final String s, final int startPos, final int endPos) {
        for (int currPos = startPos; currPos < endPos; currPos++) {
            if (Character.isAlphabetic(s.codePointAt(currPos))) {
                return true;
            }
        }
        return false;
    }

    // Lines: 12
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

    // Lines: 8 (2)
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

    public static int repeatRead = Integer.parseInt(System.getProperty("orc.test.repeatRead", "1"));

    public static int repeatCountFilename(final File file) throws IOException {
        if (!file.canRead()) {
            throw new RuntimeException("Cannot read file: " + file.getCanonicalPath());
        }
        int sum = 0;
        for (int i = 0; i < repeatRead; i++) {
            sum += countFile(file);
        }
        return sum;
    }

    public static List<File> inputList;

    public static int testPayload() throws IOException {
        int sum = 0;
        for (final File file : inputList) {
            sum += repeatCountFilename(file);
        }
        return sum;
    }

    public static void listFilesRecursively(final File startFile, final ArrayList<File> foundFiles) {
        if (startFile.isFile() && !startFile.isHidden()) {
            foundFiles.add(startFile);
        } else if (startFile.isDirectory() && !startFile.isHidden()) {
            for (final File curFile : startFile.listFiles()) {
                listFilesRecursively(curFile, foundFiles);
            }
        } else {
            /* Skip this dir. entry */
        }
    }

    public static String[] listFileNamesRecursively(final File startFile) {
        final ArrayList<File> files = new ArrayList<>();
        listFilesRecursively(startFile, files);
        final String[] fileNames = new String[files.size()];
        int i = 0;
        for (final File file : files) {
            fileNames[i] = file.getPath();
            ++i;
        }
        return fileNames;
    }

    ////////
    // Test Driver
    ////////

    private static long getProcessCumulativeCpuTime() {
        final java.lang.management.OperatingSystemMXBean osMXBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuTime();
        } else {
            return -1L;
        }
    }

    public static Long[][] timeRepetitions(final int numRepetitions) throws IOException {
        final Long[][] testElapsedTimes = new Long[numRepetitions][3];
        for (int thisRepetitionNum = 1; thisRepetitionNum <= numRepetitions; thisRepetitionNum++) {
            System.out.println("Repetition " + thisRepetitionNum + ": start.");
            final long startElapsed_ns = System.nanoTime();
            final long startCpuTime_ns = getProcessCumulativeCpuTime();
            final int p = testPayload();
            final long finishCpuTime_ns = getProcessCumulativeCpuTime();
            final long finishElapsed_ns = System.nanoTime();
            System.out.println("Repetition " + thisRepetitionNum + ": returned " + p);
            final long elapsed_us = (finishElapsed_ns - startElapsed_ns) / 1000L;
            final long cpuTime_ms = (finishCpuTime_ns - startCpuTime_ns) / 1000000L;
            System.out.println("Repetition " + thisRepetitionNum + ": finish.  Elapsed time " + elapsed_us + " µs, CPU time " + cpuTime_ms + " ms");
            testElapsedTimes[thisRepetitionNum - 1][0] = Long.valueOf(thisRepetitionNum);
            testElapsedTimes[thisRepetitionNum - 1][1] = Long.valueOf(elapsed_us);
            testElapsedTimes[thisRepetitionNum - 1][2] = Long.valueOf(cpuTime_ms);
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
        try (final OutputStream csvOut = ExecutionLogOutputStream.apply(basename, "csv", description).get(); final OutputStreamWriter csvOsw = new OutputStreamWriter(csvOut, "UTF-8");) {

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

    public static long createTestFiles(final String sourceDir, final String targetDir, final long targetFileSize, final int numFiles) throws IOException {
        final Path sourceDirPath = FileSystems.getDefault().getPath(sourceDir);
        final Path targetDirPath = FileSystems.getDefault().getPath(targetDir);
        final Path firstCopyPath = targetDirPath.resolve("input-copy-1.txt");
        try {
            Files.createDirectory(targetDirPath);
        } catch (final FileAlreadyExistsException e) {
            System.err.println("Test data directory exists, leaving unchanged: " + targetDirPath);
            final long copySize = Files.size(firstCopyPath);
            System.err.println(numFiles + " files, each " + copySize + " B = " + numFiles * copySize + " B total");
            return copySize;
        }
        System.err.println("Concatenating all files in " + sourceDirPath + " repeatedly to create input-copy-{1.." + numFiles + "}.txt (size ≥ " + targetFileSize + " B), in " + targetDirPath);
        try (final FileOutputStream firstCopyOutStream = new FileOutputStream(firstCopyPath.toFile());) {
            long bytesWritten = 0L;
            while (bytesWritten < targetFileSize) {
                for (final Path sourceFilePath : Files.newDirectoryStream(sourceDirPath)) {
                    bytesWritten += Files.copy(sourceFilePath, firstCopyOutStream);
                }
            }
        }
        for (int i = 2; i <= numFiles; i++) {
            Files.copy(firstCopyPath, targetDirPath.resolve("input-copy-" + i + ".txt"));
        }
        final long copySize = Files.size(firstCopyPath);
        System.err.println(numFiles + " files, each " + copySize + " B = " + numFiles * copySize + " B total");
        return copySize;
    }

    public static void deleteTestFiles(final String targetDir, final int numFiles) throws IOException {
        final Path targetDirPath = FileSystems.getDefault().getPath(targetDir);
        System.err.println("Deleting input-copy-{1.." + numFiles + "}.txt in " + targetDirPath);
        for (int i = 1; i <= numFiles; i++) {
            Files.deleteIfExists(targetDirPath.resolve("input-copy-" + i + ".txt"));
        }
        Files.delete(targetDirPath);
    }

    public static void main(final String[] args) throws IOException {
        setupOutput();

        final int numRepetitions = Integer.parseInt(System.getProperty("orc.test.numRepetitions", "20"));

        final String dataDir = "../OrcTests/test_data/performance/distrib/wordcount/wordcount-input-data/";
        final int numInputFiles = Integer.parseInt(System.getProperty("orc.test.numInputFiles", "12"));
        final ArrayList<File> files = new ArrayList<>();
        listFilesRecursively(new File(dataDir), files);
        inputList = files.subList(0, numInputFiles);

        final Object[][] factorValues = {
                {"Program", "WordCount.java", "", "", ""},
                {"Number of files read", Integer.valueOf(inputList.size()), "", "numInputFiles", "Words counted in this number of input text files"},
                {"Reads per file", Integer.valueOf(repeatRead), "", "repeatRead", "Number of sequential re-reads of each file"}
        };
        FactorValue.writeFactorValuesTable(factorValues);

        final Long[][] repetitionTimes = timeRepetitions(numRepetitions);

        final String[] repetitionTitles = { "Repetition number", "Elapsed time (µs)", "CPU time (ms)" };
        writeCsvFile("repetition-times", "Repetitions' elapsed times output file", repetitionTitles, repetitionTimes);
    }

}
