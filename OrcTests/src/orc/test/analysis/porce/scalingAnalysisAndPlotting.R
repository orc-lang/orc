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


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171024-a003")
#dataDir <- file.path(localExperimentDataDir, "20180130-a002")
dataDir <- file.path(localExperimentDataDir, "20180203-a009")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "Black", "Black-Scholes", levels(data$benchmarkProblemName))
  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "KMeans", "K-Means", levels(data$benchmarkProblemName))

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 3, maxRemaining = 40) %>%
    # Drop any reps which have more than 1% compilation time.
    filter(rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs) %>% bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
}

clear__ <- function() {
  rm("processedData")
}

benchmarkProperties <- {
  csv <- "
benchmarkName,                                           granularity, scalaCompute, manualPartitioning
Black-Scholes-naive (Orc),                               Tiny,        F,            F
Black-Scholes-par (Scala),                               Small,       NA,           F
Black-Scholes-partitioned-seq (Orc),                     Large,       F,            T
Black-Scholes-scala-compute (Orc),                       Large,       T,            F
Black-Scholes-scala-compute-partitioned-seq (Orc),       Large,       T,            T
Dedup-boundedchannel (Orc),                              Large,       T,            T
Dedup-boundedqueue (Scala),                              Large,       NA,           T
Dedup-naive (Orc),                                       Small,       T,            F
Dedup-nestedpar (Scala),                                 Small,       NA,           F
KMeans (Orc),                                            Small,       F,            F
KMeans-par (Scala),                                      Small,       NA,           F
KMeans-par-manual (Scala),                               Large,       NA,           T
KMeans-scala-inner (Orc),                                Large,       T,            F
SSSP-batched (Orc),                                      Small,       F,            F
SSSP-batched-par (Scala),                                Small,       NA,           F
SSSP-batched-partitioned (Orc),                          Large,       F,            T
Swaptions-naive-scala-sim (Orc),                         Small,       T,            F
Swaptions-naive-scala-subroutines-seq (Orc),             Tiny,        T,            F
Swaptions-naive-scala-swaption (Orc),                    Large,       T,            F
Swaptions-par-swaption (Scala),                          Large,       NA,           F
Swaptions-par-trial (Scala),                             Small,       NA,           F
  "
  read.csv(text = csv, strip.white = T, header = T) %>%
    replace_na(list(scalaCompute = T)) %>%
    mutate(granularity = factor(granularity, c("Tiny", "Small", "Large"), ordered = T))
}

processedData <- processedData %>%
  select(everything(), -contains("granularity"), -contains("scalaCompute"), -contains("manualPartitioning")) %>% # Strip out the data we about to add. This allows the script to be rerun without reloading the data.
  left_join(benchmarkProperties, by = "benchmarkName") %>%
  group_by(benchmarkProblemName) %>%
  addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1, granularity = "Small"), baseline = elapsedTime_mean_problembaseline) %>%
  ungroup()

plotAllData <- function(data) {
  paletteValues <- rep(c(rbind(brewer.pal(8, "Dark2"), rev(brewer.pal(12, "Set3")))), 10)
  fillPalette <- scale_fill_manual(values = paletteValues)
  colorPalette <- scale_color_manual(values = paletteValues)

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
  theme_minimal() + scale_color_brewer("Dark2")

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
    x = factor(nCPUs),
    fill = benchmarkName)) +
    labs(x = "Number of CPUs", color = "Benchmark", fill = "Benchmark") +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  p + geom_col_errorbar(aes(y = cpuUtilization / nCPUs,
                            ymin = cpuUtilization_lowerBound / nCPUs,
                            ymax = cpuUtilization_upperBound / nCPUs)) +
    labs(y = "Avg. Processor Utilization Over Run") +
    ylim(c(0, 1))
}

#print(utilizationPlot("Black"))

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


# Visualize the distribution of implementations of a given problem.

# p <- processedData %>%
#   ggplot(aes(x = benchmarkProblemName, y = elapsedTime_mean)) +
#   geom_violin(scale = "width") +
#   geom_point(position = position_jitter(0.3))
#
# print(p)
# print(p + coord_cartesian(ylim = c(0, 10)))

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

#print(elapsedTimePlots)

normalizedPerformancePlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = factor(nCPUs),
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
    fill = granularity,
    color = scalaCompute,
    group = benchmarkName,
    shape = language)) +
    labs(y = "Speed up", x = "Number of CPUs") +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  fullPerformancePlot <- p + geom_col_errorbar() + geom_point(position = position_dodge(0.9)) +
    #geom_text(aes(label = format((elapsedTime_mean_problembaseline / elapsedTime_mean), digits = 2, nsmall=0),
    #              y = pmax(pmin((elapsedTime_mean_problembaseline / elapsedTime_mean) + (0.01 * 1.5), 1.5 * 0.9), 0)),
    #          position = position_dodge(0.9), vjust = 0) +
    geom_hline(yintercept = 1, alpha = 0.4, color = "blue")

  fullPerformancePlot
}

normalizedPerformancePlots <- lapply(levels(processedData$benchmarkProblemName), normalizedPerformancePlot)

print(normalizedPerformancePlots)

sampleCountData <- processedData %>% select(benchmarkName, nCPUs, nSamples) %>% spread(nCPUs, nSamples)

sampleCountsPlot <- sampleCountData %>% ggplot(aes(
  x = factor(nCPUs),
  y = nSamples,
  fill = benchmarkName)) +
  labs(y = "Number of Samples", x = "Number of CPUs", fill = "Benchmark", shape = "Language") +
  theme_minimal() + scale_color_brewer("Dark2") + facet_wrap(~benchmarkProblemName) +
  geom_col(position = position_dodge()) +
  geom_text(aes(label = format(nSamples, digits = 2, nsmall=0),
                y = pmax(pmin(nSamples + (0.01 * 50), 50 * 0.9), 0)),
            position = position_dodge(0.9), vjust = 0)
# print(sampleCountsPlot)

# Sample count table

sampleCountTable <- function(format) {
  kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

print(levels(processedData$benchmarkProblemName))

for (problem in levels(processedData$benchmarkProblemName)) {
  ggsave(file.path(outputDir, paste0(problem, "-scaling.pdf")), scalingPlot(problem), width = 7.5, height = 6)
  ggsave(file.path(outputDir, paste0(problem, "-normPerformance.pdf")), normalizedPerformancePlot(problem), width = 7.5, height = 8)
}

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")

sampleCountTable("rst")

# Combined faceted plot for all benchmarks.

overallScalingPlot <- processedData %>% filter(scalaCompute == T | benchmarkProblemName == "SSSP", granularity != "Tiny") %>%
  ggplot(aes(
    x = factor(nCPUs),
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    #ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    #ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
    color = language,
    group = benchmarkName,
    shape = factor(if_else(manualPartitioning, "Manually Partitioned", as.character(granularity)))
    )) +
  labs(y = "Speed up", x = "Number of CPUs", shape = "Granularity", color = "Language") +
  theme_minimal() +
  scale_fill_brewer(palette="Dark2") +
  scale_color_brewer(palette="Dark2") +
  geom_point(size = 3, alpha = 0.7) +
  geom_line() +
  geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
  expand_limits(y = 0) +
  scale_y_continuous(expand = c(0, 0.5)) +
  scale_shape_manual(values = c("Large" = 0, "Manually Partitioned" = 7, "Small" = 3)) +
  facet_wrap(~benchmarkProblemName, scales = "free_y") +
  theme(
    legend.position = c(.99, .00),
    legend.justification = c("right", "bottom"),
    legend.box.just = "left",
    legend.margin = margin(3, 10, -12, 3)
  )

print(overallScalingPlot)
ggsave(file.path(outputDir, "allScalingPlot.pdf"), overallScalingPlot, width = 7.5 / 0.75, height = 4, units = "in")
