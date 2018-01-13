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
dataDir <- file.path(localExperimentDataDir, "20180109-a010")

#if(!exists("processedData"))
{
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  prunedData <- data %>% dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "gc", "minMemory", "maxMemory", "run"), rep, elapsedTime, 5, 10, 120)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, gc, minMemory, maxMemory, run) %>% bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime"), mean) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_lowerBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_upperBound) %>%
    ungroup()
}

paletteValues <- rep(c(rbind(brewer.pal(8, "Dark2"), rev(brewer.pal(12, "Set3")))), 10)
fillPalette <- scale_fill_manual(values = paletteValues)
colorPalette <- scale_color_manual(values = paletteValues)

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

processedData %>%
  mutate(group = factor(paste(benchmarkName, nCPUs, minMemory))) %>% group_by(group, maxMemory, language) %>%
  summarise_all(mean) %>%
  ggplot(aes(x = factor(maxMemory),
             y = cpuTime_mean, ymin = cpuTime_mean - gcTime_mean, ymax = cpuTime_mean,
             color = group, group = group, shape = language)) +
  geom_point(position = position_dodge(0.5), size = 4) +
  geom_errorbar(position = position_dodge(0.5))
#+ geom_line(position = position_dodge(0.1))
