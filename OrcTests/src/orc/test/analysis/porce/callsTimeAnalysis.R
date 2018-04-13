# Utilities for processing PorcE performance data.
#
# Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(knitr)
library(tidyr)
library(stringr)

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))
source(file.path(scriptDir, "porce", "utils.R"))


# The directory of a normal benchmark run
benchmarkDir <- file.path(localExperimentDataDir, "20180412-a002")

# The directory of a call profiling run
callsDir <- file.path(localExperimentDataDir, "20180412-a003")

loadBenchmarkData <- function(dataDir) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "optLevel", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95, R = 1) %>%
    # mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
    #        cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
    #        cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    ungroup()
  processedData
}

loadCallsData <- function(dataDir) {
  bdata <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  bprunedData <- bdata %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "optLevel", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01) %>%
    ungroup()

  data <- readMergedResultsTable(dataDir, "call-times", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")"))) %>%
    mutate(rep = as.integer(substring(id, 4)))

  prunedData <- left_join(bprunedData, data, by = c("benchmarkProblemName", "benchmarkName", "nPartitions", "problemSize", "work", "language", "run", "orcFile", "nCPUs", "optLevel", "rep")) %>%
    select(benchmarkProblemName, benchmarkName, nPartitions, problemSize, work, language, run, orcFile, optLevel, nCPUs, rep,
           elapsedTime, cpuTime, orcOverhead, javaOverhead, inCalls, nCalls, nJavaCalls)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, optLevel, nCPUs) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "orcOverhead", "javaOverhead", "inCalls", "nCalls", "nJavaCalls"), mean, confidence = 0.95, R = 1) %>%
    ungroup() %>%
    mutate_at(vars(contains("Overhead")), funs(. / 1000 / 1000 / 1000)) %>%
    mutate_at(vars(contains("inCalls")), funs(. / 1000 / 1000 / 1000)) %>%
    mutate(overheadPercent_mean = (orcOverhead_mean + javaOverhead_mean) / cpuTime_mean,
           overheadPercent_mean_lowerBound = (orcOverhead_mean_lowerBound + javaOverhead_mean_lowerBound) / cpuTime_mean_upperBound,
           overheadPercent_mean_upperBound = (orcOverhead_mean_upperBound + javaOverhead_mean_upperBound) / cpuTime_mean_lowerBound)

  processedData
}

loadCounterData <- function(dataDir) {
  bdata <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  bprunedData <- bdata %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "optLevel", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01) %>%
    ungroup()

  data <- readMergedResultsTable(dataDir, "counter-timers", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")"))) %>%
    mutate(rep = as.integer(substring(id, 4)))

  prunedData <- left_join(bprunedData, data, by = c("benchmarkProblemName", "benchmarkName", "nPartitions", "problemSize", "work", "language", "run", "orcFile", "nCPUs", "optLevel", "rep")) %>%
    select(benchmarkProblemName, benchmarkName, nPartitions, problemSize, work, language, run, orcFile, optLevel, nCPUs, rep,
           elapsedTime, cpuTime, time, count)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, optLevel, nCPUs) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "time", "count"), mean, confidence = 0.95, R = 1) %>%
    ungroup() %>%
    mutate_at(vars(time_mean, time_mean_lowerBound, time_mean_upperBound), funs(. / 1000 / 1000 / 1000)) %>%
    mutate(counterOverheadPercent_mean = time_mean / cpuTime_mean,
           counterOverheadPercent_mean_lowerBound = time_mean_lowerBound / cpuTime_mean_upperBound,
           counterOverheadPercent_mean_upperBound = time_mean_upperBound / cpuTime_mean_lowerBound)

  processedData
}

benchmarkData <- loadBenchmarkData(benchmarkDir)

callsData <- loadCallsData(callsDir) %>%
  select(benchmarkProblemName, benchmarkName, language, optLevel, nCPUs,
         overheadPercent_mean, overheadPercent_mean_lowerBound, overheadPercent_mean_upperBound)

counterData <- loadCounterData(callsDir) %>%
  select(benchmarkProblemName, benchmarkName, language, optLevel, nCPUs,
         counterOverheadPercent_mean, counterOverheadPercent_mean_lowerBound, counterOverheadPercent_mean_upperBound)

processedData <- full_join(full_join(benchmarkData, callsData, by = c("benchmarkProblemName", "language", "benchmarkName", "nCPUs", "optLevel")),
                           counterData, by = c("benchmarkProblemName", "language", "benchmarkName", "nCPUs", "optLevel")) %>%
  mutate(compensatedElapsedTime_mean = elapsedTime_mean * (1 - overheadPercent_mean - counterOverheadPercent_mean),
         compensatedElapsedTime_mean_upperBound = elapsedTime_mean_upperBound * (1 - overheadPercent_mean_lowerBound - counterOverheadPercent_mean_lowerBound),
         compensatedElapsedTime_mean_lowerBound = elapsedTime_mean_lowerBound * (1 - overheadPercent_mean_upperBound - counterOverheadPercent_mean_upperBound)) %>%
  select(benchmarkProblemName, benchmarkName, language, optLevel, nCPUs,
         elapsedTime_mean, elapsedTime_mean_lowerBound, elapsedTime_mean_upperBound,
         compensatedElapsedTime_mean, compensatedElapsedTime_mean_lowerBound, compensatedElapsedTime_mean_upperBound,
         cpuTime_mean, cpuTime_mean_lowerBound, cpuTime_mean_upperBound,
         overheadPercent_mean, overheadPercent_mean_lowerBound, overheadPercent_mean_upperBound,
         counterOverheadPercent_mean, counterOverheadPercent_mean_lowerBound, counterOverheadPercent_mean_upperBound,
         nSamples)

# t <- processedData %>%
#   transmute(benchmarkProblemName, benchmarkName,
#             elapsedTime = signif(elapsedTime_mean, 7), compensatedElapsedTime = signif(compensatedElapsedTime_mean, 7),
#             invokationOverheadPercent = round(overheadPercent_mean * 100, 3), counterOverheadPercent = round(counterOverheadPercent_mean * 100, 3),
#             nSamples)

t <- processedData %>%
  transmute(benchmarkProblemName, benchmarkName, optLevel, nCPUs,
            elapsedTime_mean, elapsedTime_mean_lowerBound, elapsedTime_mean_upperBound,
            compensatedElapsedTime_mean, compensatedElapsedTime_mean_lowerBound, compensatedElapsedTime_mean_upperBound,
            invokationOverhead_mean = overheadPercent_mean, invokationOverhead_mean_lowerBound = overheadPercent_mean_lowerBound, invokationOverhead_mean_upperBound = overheadPercent_mean_upperBound,
            counterOverhead_mean = counterOverheadPercent_mean, counterOverhead_mean_lowerBound = counterOverheadPercent_mean_lowerBound, counterOverhead_mean_upperBound = counterOverheadPercent_mean_upperBound,
            nSamples)

# Output

timeOutputDir <- file.path(benchmarkDir, "time")
if (!dir.exists(timeOutputDir)) {
  dir.create(timeOutputDir)
}

print(t)

write.csv(t, file = file.path(timeOutputDir, "mean_compensated_elapsed_time.csv"), row.names = F)

