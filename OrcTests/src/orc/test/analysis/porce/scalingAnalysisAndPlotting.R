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


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171015-a001-merged")
dataDir <- file.path(localExperimentDataDir, "20171024-a003")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  prunedData <- data %>% dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs"), rep, elapsedTime, 5, 50, 120)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs) %>% bootstrapStatistics(c("elapsedTime", "cpuTime"), mean) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_lowerBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_upperBound) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
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
  x = factor(nCPUs),
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


utilizationPlot <- p + geom_col_errorbar(aes(y = cpuUtilization / nCPUs,
                                             ymin = cpuUtilization_lowerBound / nCPUs,
                                             ymax = cpuUtilization_upperBound / nCPUs)) +
  labs(y = "Avg. Processor Utilization Over Run") +
  ylim(c(0, 1))

#print(utilizationPlot)

## Seperate Scaling Plots for each problem.

scalingPlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = nCPUs,
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
    fill = benchmarkName,
    color = benchmarkName,
    linetype = language)) +
    labs(y = "Speed up", x = "Number of CPUs", color = "Benchmark", fill = "Benchmark", linetype = "Language") +
    ggtitle(problemName) +
    theme_minimal() + scale_color_brewer(palette="Dark2")

  scalingPlot <- p + geom_line() + geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
    scale_x_continuous_breaks_from(breaks_from = processedData$nCPUs)

  scalingPlot
}

scalingPlots <- lapply(levels(processedData$benchmarkProblemName), scalingPlot)

# print(scalingPlots)

elapsedTimePlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = factor(nCPUs),
    y = elapsedTime_mean,
    ymin = elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_upperBound,
    fill = benchmarkName,
    shape = language)) +
    labs(y = "Elapsed Execution Time (s)", x = "Number of CPUs", fill = "Benchmark", shape = "Language") +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  fullPerformancePlot <- p + geom_col_errorbar() + geom_point(position = position_dodge(0.9)) +
    geom_text(aes(label = format(elapsedTime_mean, digits = 2, nsmall=0),
                  y = pmax(pmin(elapsedTime_mean + (0.01 * max(elapsedTime_mean)), max(elapsedTime_mean) * 0.9), 0)),
              position = position_dodge(0.9), vjust = 0)

  fullPerformancePlot
}

elapsedTimePlots <- lapply(levels(processedData$benchmarkProblemName), elapsedTimePlot)

# print(elapsedTimePlots)

normalizedPerformancePlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = factor(nCPUs),
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
    fill = benchmarkName,
    shape = language)) +
    labs(y = "Speed up over Scala", x = "Number of CPUs", fill = "Benchmark", shape = "Language") +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  fullPerformancePlot <- p + geom_col_errorbar() + geom_point(position = position_dodge(0.9)) +
    geom_text(aes(label = format((elapsedTime_mean_problembaseline / elapsedTime_mean), digits = 2, nsmall=0),
                  y = pmax(pmin((elapsedTime_mean_problembaseline / elapsedTime_mean) + (0.01 * 1.5), 1.5 * 0.9), 0)),
              position = position_dodge(0.9), vjust = 0) +
    geom_hline(yintercept = 1, alpha = 0.4, color = "blue")

  fullPerformancePlot
}

normalizedPerformancePlots <- lapply(levels(processedData$benchmarkProblemName), normalizedPerformancePlot)

#print(normalizedPerformancePlots)

sampleCountsPlot <- sampleCounts %>% ggplot(aes(
  x = factor(nCPUs),
  y = nSamples,
  fill = benchmarkName)) +
  labs(y = "Speed up over Scala", x = "Number of CPUs", fill = "Benchmark", shape = "Language") +
  theme_minimal() + fillPalette + facet_wrap(~benchmarkProblemName) +
  geom_col(position = position_dodge()) +
  geom_text(aes(label = format(nSamples, digits = 2, nsmall=0),
                y = pmax(pmin(nSamples + (0.01 * 50), 50 * 0.9), 0)),
            position = position_dodge(0.9), vjust = 0)
# print(sampleCountsPlot)

# Sample count table

sampleCountData <- processedData %>% select(benchmarkName, nCPUs, nSamples) %>% spread(nCPUs, nSamples)

sampleCountTable <- function(format) {
  kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

for (problem in levels(processedData$benchmarkProblemName)) {
  ggsave(file.path(outputDir, paste0(problem, "-scaling.pdf")), scalingPlot(problem), width = 7.5, height = 6)
  ggsave(file.path(outputDir, paste0(problem, "-normPerformance.pdf")), normalizedPerformancePlot(problem), width = 7.5, height = 8)
}

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")
capture.output(sampleCountTable("latex"), file = file.path(outputDir, "usedSampleCounts.tex"), type = "output")



## Facetted plots

# p <- processedData %>% ggplot(aes(
#     y = elapsedTime_mean_selfbaseline / elapsedTime_mean,
#     ymin = elapsedTime_mean_selfbaseline / elapsedTime_mean_lowerBound,
#     ymax = elapsedTime_mean_selfbaseline / elapsedTime_mean_upperBound,
#     fill = benchmarkName)) + facet_wrap(~benchmarkProblemName) +
#   labs(y = "Speed up", x = "Number of CPUs", color = "Benchmark", fill = "Benchmark") +
#   theme_minimal() + fillPalette + colorPalette
#
# selfScalingPlot <- p + geom_line(aes(x = nCPUs, color = benchmarkName)) + scale_x_continuous_breaks_from(breaks_from = processedData$nCPUs)
# #fullPerformancePlot <- p + geom_col_errorbar(aes(x = factor(nCPUs)))
#
# # This plot is probably useless.
# p <- processedData %>% ggplot(aes(
#   y = elapsedTime_mean_problembaseline / elapsedTime_mean,
#   ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
#   ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
#   fill = benchmarkName)) +
#   labs(y = "Speed up", x = "Number of CPUs", color = "Benchmark", fill = "Benchmark") +
#   theme_minimal() + fillPalette + colorPalette + facet_wrap(~benchmarkProblemName)
#
# scalingPlot <- p + geom_line(aes(x = nCPUs, color = benchmarkName)) +
#   geom_point(aes(x = nCPUs, color = benchmarkName, shape = language)) +
#   scale_x_continuous_breaks_from(breaks_from = processedData$nCPUs) +
#   geom_hline(yintercept = 1, alpha = 0.5, color = "blue")
# fullPerformancePlot <- p + geom_col_errorbar(aes(x = factor(nCPUs)))

#print(scalingPlot)
#print(fullPerformancePlot)

#ggsave(file.path(dataDir, "scalingPlot.pdf"), scalingPlot, width = 7.5, height = 6)
#ggsave(file.path(dataDir, "fullPlot.pdf"), fullPerformancePlot, width = 7.5, height = 8)


