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


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")
dataDir <- file.path(localExperimentDataDir, "20180220-a002")
#dataDir <- file.path(localExperimentDataDir, "20180203-a009")

loadData <- function(dataDir) {
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
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs) %>% bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    #group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
  processedData
}

if(!exists("processedData")) {
  # processedData <- loadData(file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")) %>% mutate(experiment = "old")
  # processedData <- processedData %>%
  #   rbind(loadData(file.path(localExperimentDataDir, "20180220-a002")) %>% mutate(experiment = "new"))
  # processedData <- processedData %>% mutate(experiment = factor(experiment, ordered = T, levels = c("old", "new")))
  processedData <- loadData(dataDir) %>% mutate(experiment = factor("only"))
}

clear__ <- function() {
  rm("processedData")
}

print(levels(processedData$benchmarkName))

benchmarkProperties <- {
  csv <- "
benchmarkName,                                           granularity, scalaCompute, parallelism, isBaseline
Black-Scholes-naive (Orc),                               Fine,        F,            Naïve,       F
Black-Scholes-partially-seq (Orc),                       Fine,        F,            Naïve,       F
Black-Scholes-par (Scala),                               Fine,        NA,           Par. Col.,   TRUE
Black-Scholes-partitioned-seq (Orc),                     Coarse,      F,            Partition,   F
Black-Scholes-scala-compute (Orc),                       Fine,        T,            Naïve,       F
Black-Scholes-scala-compute-partially-seq (Orc),         Fine,        T,            Naïve,       F
Black-Scholes-scala-compute-partitioned-seq (Orc),       Coarse,      T,            Partition,   F
Dedup-boundedchannel (Orc),                              Coarse,      T,            Thread,      F
Dedup-boundedqueue (Scala),                              Coarse,      NA,           Thread,      F
Dedup-naive (Orc),                                       Fine,        T,            Naïve,       F
Dedup-nestedpar (Scala),                                 Fine,        NA,           Par. Col.,   TRUE
KMeans (Orc),                                            Fine,        F,            Naïve,       F
KMeans-par (Scala),                                      Fine,        NA,           Par. Col.,   TRUE
KMeans-par-manual (Scala),                               Coarse,      NA,           Partition,   F
KMeans-scala-inner (Orc),                                Coarse,      T,            Partition,   F
SSSP-batched (Orc),                                      Fine,        F,            Naïve,       F
SSSP-batched-par (Scala),                                Fine,        NA,           Par. Col.,   TRUE
SSSP-batched-partitioned (Orc),                          Coarse,      F,            Partition,   F
Swaptions-naive-scala-sim (Orc),                         Coarse,      T,            Naïve,       F
Swaptions-naive-scala-subroutines-seq (Orc),             Fine,        T,            Naïve,       F
Swaptions-naive-scala-swaption (Orc),                    Coarse,      T,            Naïve,       F
Swaptions-par-swaption (Scala),                          Coarse,      NA,           Par. Col.,   F
Swaptions-par-trial (Scala),                             Coarse,      NA,           Par. Col.,   TRUE
  "
  r <- read.csv(text = csv, strip.white = T, header = T) %>%
    replace_na(list(scalaCompute = T)) %>%
    mutate(granularity = factor(granularity, c("Super Fine", "Fine", "Coarse"), ordered = T)) %>%
    mutate(parallelism = factor(if_else(parallelism == "Par. Col.", "Naïve", as.character(parallelism))))

  levels(r$granularity) <- c("S. Fine", "Fine", "Coarse")

  r
}

processedData <- processedData %>%
  select(everything(), -contains("granularity"), -contains("scalaCompute"), -contains("parallelism"), -contains("isBaseline")) %>% # Strip out the data we about to add. This allows the script to be rerun without reloading the data.
  left_join(benchmarkProperties, by = "benchmarkName") %>%
  group_by(benchmarkProblemName) %>%
  addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1, isBaseline = T, experiment = levels(processedData$experiment)[1]), baseline = elapsedTime_mean_problembaseline) %>%
  ungroup()

includedBenchmarks <- {
  txt <- "
  #Black-Scholes-naive (Orc)
  #Black-Scholes-partially-seq (Orc)
  Black-Scholes-par (Scala)
  Black-Scholes-partitioned-seq (Orc)
  #Black-Scholes-scala-compute (Orc)
  Black-Scholes-scala-compute-partially-seq (Orc)
  Black-Scholes-scala-compute-partitioned-seq (Orc)
  Dedup-boundedchannel (Orc)
  Dedup-boundedqueue (Scala)
  Dedup-naive (Orc)
  Dedup-nestedpar (Scala)
  KMeans (Orc)
  KMeans-par (Scala)
  KMeans-par-manual (Scala)
  KMeans-scala-inner (Orc)
  SSSP-batched (Orc)
  SSSP-batched-par (Scala)
  SSSP-batched-partitioned (Orc)
  Swaptions-naive-scala-sim (Orc)
  Swaptions-naive-scala-subroutines-seq (Orc)
  #Swaptions-naive-scala-swaption (Orc)
  #Swaptions-par-swaption (Scala)
  Swaptions-par-trial (Scala)
"
  read.csv(text = txt, strip.white = T, header = F, stringsAsFactors = F)[[1]]
}

processedData <- processedData %>% filter(is.element(benchmarkName, includedBenchmarks))

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
    linetype = parallelism,
    fill = factor(if_else((language == "Orc") & scalaCompute, "Orc+Scala", as.character(language)), levels = c("Orc+Scala", "Orc", "Scala")),
    color = granularity,
    group = benchmarkName)) +
    labs(y = "Speed up", x = "Number of CPUs", fill = "Language") +
    theme_minimal() + scale_fill_brewer(palette="Dark2")

  fullPerformancePlot <- p + geom_col_errorbar() +
    #geom_text(aes(label = format((elapsedTime_mean_problembaseline / elapsedTime_mean), digits = 2, nsmall=0),
    #              y = pmax(pmin((elapsedTime_mean_problembaseline / elapsedTime_mean) + (0.01 * 1.5), 1.5 * 0.9), 0)),
    #          position = position_dodge(0.9), vjust = 0) +
    geom_hline(yintercept = 1, alpha = 0.4, color = "blue")

  fullPerformancePlot
}

normalizedPerformancePlots <- lapply(levels(processedData$benchmarkProblemName), normalizedPerformancePlot)

# print(normalizedPerformancePlots)

sampleCountData <- processedData %>% select(experiment, benchmarkName, nCPUs, nSamples) %>% spread(nCPUs, nSamples)

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

# for (problem in levels(processedData$benchmarkProblemName)) {
#   ggsave(file.path(outputDir, paste0(problem, "-scaling.pdf")), scalingPlot(problem), width = 7.5, height = 6)
#   ggsave(file.path(outputDir, paste0(problem, "-normPerformance.pdf")), normalizedPerformancePlot(problem), width = 7.5, height = 8)
# }

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")

sampleCountTable("rst")

# Combined faceted plot for all benchmarks.

overallScalingPlot <- processedData %>%
  ggplot(aes(
    x = factor(nCPUs),
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    #ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    #ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
    color = parallelism,
    group = if(levels(experiment) == c("only")) benchmarkName else factor(paste(experiment, benchmarkName)),
    linetype = factor(if_else((language == "Orc") & scalaCompute, "Orc+Scala", as.character(language)), levels = c("Orc+Scala", "Orc", "Scala"))
    )) +
  labs(y = "Speed Up", x = "Number of Cores", shape = "Granularity", linetype = "Language", color = "Parallelism") +
  theme_minimal() +
  #scale_fill_brewer(palette="Set3") +
  #scale_color_brewer(palette="PuBuGn", direction = -1) +
  scale_color_manual(values = c("#555555", "#E69F00", "#56B4E9")) + # "#67a9cf", "#1c9099", "#016c59"
  # geom_point(data = processedData %>% filter(benchmarkProblemName != "Swaptions"), alpha = 0.5, shape = 4) +
  geom_point(aes(shape = if(levels(experiment) == c("only")) granularity else factor(paste(experiment, granularity))),
             processedData, alpha = 0.7) +
  geom_line() +
  geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
  expand_limits(y = 0) +
  scale_y_continuous(expand = c(0, 0.5)) +
  scale_shape_discrete(solid = F) +
  facet_wrap(~benchmarkProblemName, scales = "free_y", nrow = 1) +
  theme(
    #legend.justification = c("right", "top"),
    #legend.box.just = "top",
    legend.margin = margin(-8, 0, 0, -30),
    legend.direction = "horizontal",
    #legend.box = "vertical",
    legend.box = "horizontal",
    legend.spacing = grid::unit(45, "points"),
    text = element_text(size=9),
    legend.text = element_text(size=8),
    strip.text = element_text(size=9)
  )

print(overallScalingPlot + theme(legend.position = "bottom"))
ggsave(file.path(outputDir, "allScalingPlot.pdf"), overallScalingPlot + theme(legend.position = "bottom"), width = 7.5, height = 2, units = "in")

# svg( file.path(outputDir, "allScalingPlot-legend.svg"), width = 7.5, height = 2 )
# print(overallScalingPlot + theme(legend.position = "bottom"))
# dev.off()
