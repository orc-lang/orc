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
dataDir <- file.path(localExperimentDataDir, "20171112-a002")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])))

  prunedData <- data %>% dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "step"), rep, elapsedTime, 5, 50, 120)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, step) %>% bootstrapStatistics(c("elapsedTime", "cpuTime"), mean) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_lowerBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_upperBound) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(step=0.0), baseline = elapsedTime_mean_selfbaseline) %>%
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

p <- processedData %>% ggplot(aes(
  x = factor(step),
  fill = benchmarkName)) +
  labs(x = "Number of CPUs", color = "Benchmark", fill = "Benchmark") +
  theme_minimal() + fillPalette

timeAndUtilizationPlot <- p +
  geom_col_errorbar(aes(y = elapsedTime_mean,
                        ymin = elapsedTime_mean_lowerBound,
                        ymax = elapsedTime_mean_upperBound)) +
  geom_col_errorbar(aes(y = cpuTime_mean / nCPUs,
                        ymin = cpuTime_mean_lowerBound / nCPUs,
                        ymax = cpuTime_mean_upperBound / nCPUs
  ), color="white") +
  labs(y = "Elapsed Time (white mark: time fully utilizing CPUs)")

#print(timeAndUtilizationPlot)

utilizationPlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = factor(step),
    fill = benchmarkName)) +
    labs(x = "Number of CPUs", color = "Benchmark", fill = "Benchmark") +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  p + geom_col_errorbar(aes(y = cpuUtilization / nCPUs,
                            ymin = cpuUtilization_lowerBound / nCPUs,
                            ymax = cpuUtilization_upperBound / nCPUs)) +
    labs(y = "Avg. Processor Utilization Over Run") +
    ylim(c(0, 1))
}

## Seperate Scaling Plots for each problem.

scalingPlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = factor(step),
    y = elapsedTime_mean_selfbaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_selfbaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_selfbaseline / elapsedTime_mean_upperBound,
    fill = benchmarkName)) +
    labs(y = "Speed-up w.r.t. time limit = 0 (large dark bar)\nCPU Utilization Ratio (small light bar)", x = "Time Limit", fill = "Benchmark") +
    ggtitle(problemName) +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  scalingPlot <- p + geom_col_errorbar() +
    geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
    geom_col_errorbar(aes(y = cpuUtilization / nCPUs,
                          ymin = cpuUtilization_lowerBound / nCPUs,
                          ymax = cpuUtilization_upperBound / nCPUs
                        ), fill="white", width = 0.2, alpha = 0.5)

  scalingPlot
}

scalingPlots <- lapply(levels(processedData$benchmarkProblemName), scalingPlot)


p <- processedData %>% ggplot(aes(
    y = elapsedTime_mean_selfbaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_selfbaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_selfbaseline / elapsedTime_mean_upperBound,
    fill = benchmarkName)) + facet_wrap(~benchmarkProblemName) +
  labs(y = "Speed up", x = "Time Limit", color = "Benchmark", fill = "Benchmark") +
  theme_minimal() + fillPalette + colorPalette

selfScalingPlot <- p + geom_line(aes(x = factor(step), color = benchmarkName))
#fullPerformancePlot <- p + geom_col_errorbar(aes(x = factor(nCPUs)))
