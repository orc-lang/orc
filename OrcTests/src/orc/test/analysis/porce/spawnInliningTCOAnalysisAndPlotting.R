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

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171024-a003")
dataDir <- file.path(localExperimentDataDir, "20180203-a007")

nameOrNothing <- function(v) {
  name <- deparse(substitute(v))
  if_else(v, name, "")
}

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times") %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])),
           universalTCO = as.logical(universalTCO),
           actuallySchedule = as.logical(actuallySchedule),
           allowAllSpawnInlining = as.logical(allowAllSpawnInlining))

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "universalTCO", "actuallySchedule", "allowAllSpawnInlining"), rep, elapsedTime, 5, 50, 120,
                                   minRemaining = 1, maxRemaining = 5)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, universalTCO, actuallySchedule, allowAllSpawnInlining) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime"), mean, R = 0) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_lowerBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_upperBound) %>%
    ungroup() %>%
    mutate(configuration = factor(paste0(nameOrNothing(universalTCO), "/", nameOrNothing(actuallySchedule), "/", nameOrNothing(allowAllSpawnInlining))))
    # group_by(benchmarkProblemName, language) %>% mutate_if(is.numeric, funs(. / mean(.)))
}

plotAllData <- function(data) {
  d <- data %>% group_by(benchmarkName, nCPUs) %>% mutate(norm = elapsedTime / min(elapsedTime), startTime = cumsum(elapsedTime))
  vis <- list(geom_point(alpha = 0.8, position = position_jitter()), colorPalette)
  p <- d %>% ggplot(aes(x = rep, y = norm, color = benchmarkName)) + vis
  print(p)
  p <- d %>% ggplot(aes(x = startTime, y = norm, color = benchmarkName)) + vis
  print(p)
}

# Turn on to view data and evaluate the number warm up iterations.
#plotAllData(data)

p <- processedData %>% ggplot(aes(
  x = configuration,
  fill = benchmarkName)) +
  theme_minimal() + scale_fill_brewer(palette="Dark2") +
  theme(axis.text.x = element_text(angle = 40, hjust = 1)) +
  facet_wrap(~benchmarkName, scales = "free_y")

timeAndUtilizationPlot <- p +
  geom_col_errorbar(aes(y = elapsedTime_mean,
                        ymin = elapsedTime_mean_lowerBound,
                        ymax = elapsedTime_mean_upperBound)) +
  geom_col_errorbar(aes(y = cpuTime_mean / nCPUs,
                        ymin = cpuTime_mean_lowerBound / nCPUs,
                        ymax = cpuTime_mean_upperBound / nCPUs
  ), color="white", fill="black", alpha = 0.5) +
  labs(y = "Elapsed Time (white mark: time fully utilizing CPUs)")

print(timeAndUtilizationPlot)
