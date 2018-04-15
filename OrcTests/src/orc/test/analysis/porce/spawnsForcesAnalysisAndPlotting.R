# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")
dataDir <- file.path(localExperimentDataDir, "20180413-a001")

loadData <- function(dataDir) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  rawData <<- data

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "optLevel", "run", "optSpawns", "optFutures"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel, optSpawns, optFutures) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    ungroup()
  processedData
}

if(!exists("processedData")) {
  processedData <- loadData(dataDir)
}

print(levels(processedData$benchmarkName))

benchmarkProperties <- {
  r <- read.csv(file.path(scriptDir, "porce", "benchmark-metadata.csv"), strip.white = T, header = T) %>%
    replace_na(list(scalaCompute = T)) %>%
    mutate(granularity = factor(granularity, c("Super Fine", "Fine", "Coarse"), ordered = T)) %>%
    mutate(parallelism = factor(if_else(parallelism == "Par. Col.", "Naïve", as.character(parallelism))))

  levels(r$granularity) <- c("S. Fine", "Fine", "Coarse")

  r
}

processedData <- processedData %>%
  select(everything(), -contains("granularity"), -contains("scalaCompute"), -contains("parallelism"), -contains("isBaseline"), -contains("implType"), -contains("optimized")) %>% # Strip out the data we about to add. This allows the script to be rerun without reloading the data.
  left_join(benchmarkProperties, by = c("benchmarkName")) %>%
  group_by(benchmarkProblemName) %>%
  #addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1, isBaseline = T, experiment = levels(processedData$experiment)[1]), baseline = elapsedTime_mean_problembaseline) %>%
  ungroup()

# Sample count table

sampleCountData <- processedData %>%
  mutate(implType = factor(if_else(implType == "Orc", paste0("Orc -O", optLevel), as.character(implType)))) %>%
  select(benchmarkName, nCPUs, implType, nSamples)

sampleCountTable <- function(format) {
  kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

timeOutputDir <- file.path(dataDir, "time")
if (!dir.exists(timeOutputDir)) {
  dir.create(timeOutputDir)
}

print(levels(processedData$benchmarkProblemName))

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")

sampleCountTable("rst")

processedData %>% group_by(benchmarkName) %>% addBaseline(cpuTime_mean, c(optSpawns = "false", optFutures = "false")) %>% ungroup() %>%
  #dropBounds() %>%
  #select(-cpuTime_mean, gcTime_mean, -confidence, -nSamples, -granularity, -nCPUs, -optLevel, -benchmarkName, -isBaseline, -optimized, -implType, -parallelism, -scalaCompute) %>%
  ggplot(aes(x = factor(paste(optSpawns, optFutures)), y = cpuTime_mean / cpuTime_mean_baseline, group = benchmarkName)) +
  geom_line() +
  #geom_jitter(width = 0.1, height = 0, alpha = 0.3, size = 10)
  geom_point(alpha = 0.3, size = 10) +
  geom_smooth(aes(group = 0), se = F)

processedData %>%
  ggplot(aes(x = factor(paste(optSpawns, optFutures)), y = cpuUtilization, group = 0)) +
  geom_jitter(width = 0.1, height = 0, alpha = 0.3, size = 10) +
  geom_smooth(se = F)
