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


dataDir <- file.path(localExperimentDataDir, "20180414-a006")

processedData <-
  loadAndPruneBenchmarkData(dataDir, benchmarkIDCols = c("benchmarkName", "nCPUs", "optLevel", "spawnLimit", "optFutures", "run")) %>%
  group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(spawnLimit = 0.1))

print(
  processedData %>%
    ggplot(aes(factor(spawnLimit), elapsedTime_mean / elapsedTime_mean_baseline - as.integer(benchmarkProblemName)*2,
               color = benchmarkProblemName,
               group = paste(benchmarkProblemName, run))) +
    geom_line()
)

processedData %>% group_by(spawnLimit) %>% summarise(geomean(elapsedTime_mean / elapsedTime_mean_baseline))

processedData %>% group_by(benchmarkName) %>% summarise(spawnLimit[which.min(elapsedTime_mean)])

print(
  processedData %>%
    ggplot(aes(factor(spawnLimit), cpuUtilization, color = benchmarkProblemName, group = paste(benchmarkProblemName, run))) +
    geom_line()
)
