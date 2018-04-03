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

# The directory of a normal benchmark run
benchmarkDir <- file.path(localExperimentDataDir, "20180402-a001")

# The directory of a call profiling run
callsDir <- file.path(localExperimentDataDir, "20180402-a001")

loadBenchmarkData <- function(dataDir) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "Black", "Black-Scholes", levels(data$benchmarkProblemName))
  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "KMeans", "K-Means", levels(data$benchmarkProblemName))

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    filter(rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95, R = 1) %>%
    # mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
    #        cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
    #        cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    ungroup()
  processedData
}

loadCallsData <- function(dataDir) {
  bdata <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  levels(bdata$benchmarkProblemName) <- if_else(levels(bdata$benchmarkProblemName) == "Black", "Black-Scholes", levels(bdata$benchmarkProblemName))
  levels(bdata$benchmarkProblemName) <- if_else(levels(bdata$benchmarkProblemName) == "KMeans", "K-Means", levels(bdata$benchmarkProblemName))

  bprunedData <- bdata %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    filter(rtCompTime < cpuTime * 0.01)

  data <- readMergedResultsTable(dataDir, "call-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")"))) %>%
    mutate(rep = as.integer(substring(id, 4)))


  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "Black", "Black-Scholes", levels(data$benchmarkProblemName))
  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "KMeans", "K-Means", levels(data$benchmarkProblemName))

  prunedData <- left_join(bprunedData, data) %>%
    select(benchmarkProblemName, benchmarkName, nPartitions, problemSize, work, language, run, orcFile, nCPUs, rep,
           elapsedTime, cpuTime, orcOverhead, javaOverhead, inCalls, nCalls, nJavaCalls)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "orcOverhead", "javaOverhead", "inCalls", "nCalls", "nJavaCalls"), mean, confidence = 0.95, R = 1) %>%
    ungroup() %>%
    mutate_at(vars(contains("Overhead")), funs(. / 1000 / 1000 / 1000)) %>%
    mutate_at(vars(contains("inCalls")), funs(. / 1000 / 1000 / 1000)) %>%
    mutate(overheadPercent_mean = (orcOverhead_mean + javaOverhead_mean) / cpuTime_mean,
           overheadPercent_mean_lowerBound = (orcOverhead_mean_lowerBound + javaOverhead_mean_lowerBound) / cpuTime_mean_upperBound,
           overheadPercent_mean_upperBound = (orcOverhead_mean_upperBound + javaOverhead_mean_upperBound) / cpuTime_mean_lowerBound)

  processedData
}

callsData <- loadCallsData(callsDir) %>%
  select(benchmarkProblemName, benchmarkName, language, nCPUs,
         overheadPercent_mean, overheadPercent_mean_lowerBound, overheadPercent_mean_upperBound, nSamples)
benchmarkData <- loadBenchmarkData(callsDir)

processedData <- full_join(benchmarkData, callsData) %>%
  mutate(compensatedElapsedTime_mean = elapsedTime_mean * (1 - overheadPercent_mean),
         compensatedElapsedTime_mean_upperBound = elapsedTime_mean_upperBound * (1 - overheadPercent_mean_lowerBound),
         compensatedElapsedTime_mean_lowerBound = elapsedTime_mean_lowerBound * (1 - overheadPercent_mean_upperBound)) %>%
  select(benchmarkProblemName, benchmarkName, language, nCPUs,
         elapsedTime_mean, elapsedTime_mean_lowerBound, elapsedTime_mean_upperBound,
         compensatedElapsedTime_mean, compensatedElapsedTime_mean_lowerBound, compensatedElapsedTime_mean_upperBound,
         cpuTime_mean, cpuTime_mean_lowerBound, cpuTime_mean_upperBound,
         overheadPercent_mean, overheadPercent_mean_lowerBound, overheadPercent_mean_upperBound,
         nSamples)

View(processedData)
