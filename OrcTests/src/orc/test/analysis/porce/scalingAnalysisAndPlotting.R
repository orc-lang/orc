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
dataDir <- file.path(localExperimentDataDir, "20180412-a002")

loadData <- function(dataDir) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  #d <<- data

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "optLevel", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    #group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
  processedData
}

loadCompensatedData <- function(dataDir) {
  source(file.path(scriptDir, "porce", "callsTimeAnalysis.R"), local=new.env())
  read.csv(file.path(dataDir, "time", "mean_compensated_elapsed_time.csv"), header = T) %>%
    select(everything(),
           #-elapsedTime_mean, -elapsedTime_mean_lowerBound, -elapsedTime_mean_upperBound,
           -nSamples)
}

if(!exists("processedData")) {
  #processedData <- loadData(file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")) %>% mutate(experiment = "old")
  #processedData <- processedData %>%
  #   rbind(loadData(file.path(localExperimentDataDir, "20180220-a002")) %>% mutate(experiment = "new"))
  #processedData <- processedData %>% mutate(experiment = factor(experiment, ordered = T, levels = c("old", "new")))
  processedData <- loadData(dataDir) %>% mutate(experiment = factor("only"))
  compensatedData <- loadCompensatedData(dataDir)
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
  select(-contains("granularity"), -contains("scalaCompute"), -contains("parallelism"), -contains("isBaseline"), -contains("implType"), -contains("optimized")) %>% # Strip out the data we about to add. This allows the script to be rerun without reloading the data.
  left_join(benchmarkProperties, by = c("benchmarkName")) %>%
  group_by(benchmarkProblemName) %>%
  #addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1, isBaseline = T, experiment = levels(processedData$experiment)[1]), baseline = elapsedTime_mean_problembaseline) %>%
  ungroup()

# Sample count table

sampleCountData <- processedData %>%
  mutate(implType = factor(if_else(implType == "Orc", paste0("Orc -O", optLevel), as.character(implType)))) %>%
  select(experiment, benchmarkName, nCPUs, implType, nSamples)

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

# for (problem in levels(processedData$benchmarkProblemName)) {
#   ggsave(file.path(outputDir, paste0(problem, "-scaling.pdf")), scalingPlot(problem), width = 7.5, height = 6)
#   ggsave(file.path(outputDir, paste0(problem, "-normPerformance.pdf")), normalizedPerformancePlot(problem), width = 7.5, height = 8)
# }

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")

sampleCountTable("rst")

# Benchmark vs. Implementation table

useNCPUs <- max(processedData$nCPUs)
useOptLevel <- max(processedData$optLevel, na.rm = T)

implTypes <- c("Orc -O0", paste0("Orc -O", useOptLevel), "Orc*", "Orc+Scala", "Scala")

compensatedRows <- right_join(processedData, compensatedData, by = c("benchmarkProblemName", "benchmarkName", "nCPUs", "optLevel")) %>%
  filter(implType == "Orc", optLevel == useOptLevel) %>%
  mutate(implType = "Orc*",
         elapsedTime_mean = compensatedElapsedTime_mean,
         elapsedTime_mean_upperBound = compensatedElapsedTime_mean_upperBound,
         elapsedTime_mean_lowerBound = compensatedElapsedTime_mean_lowerBound
        ) %>%
  select(colnames(processedData))

longT <- processedData %>% rbind(compensatedRows) %>% filter(nCPUs == useNCPUs) %>%
  mutate(implType = factor(if_else(implType == "Orc", paste0("Orc -O", optLevel), as.character(implType)), levels = implTypes)) %>%
  mutate(implType = if_else(optimized, paste0(implType, " Opt"), as.character(implType)))

t <- longT %>%
  filter(optimized | language == "Scala") %>%
  select(benchmarkProblemName, implType, elapsedTime_mean) %>% spread(implType, elapsedTime_mean)

write.csv(t, file = file.path(timeOutputDir, "mean_elapsed_time.csv"), row.names = F)

print(t)
print(kable(t, "latex"))

t <- longT %>%
  filter(!optimized | language == "Scala") %>%
  select(benchmarkProblemName, implType, elapsedTime_mean) %>% spread(implType, elapsedTime_mean)

print(t)

#processedData <- processedData %>% filter(is.element(benchmarkName, includedBenchmarks))
#processedData <- processedData %>% filter(optimized == F)

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

#scalingPlots <- lapply(levels(processedData$benchmarkProblemName), scalingPlot)


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

#elapsedTimePlots <- lapply(levels(processedData$benchmarkProblemName), elapsedTimePlot)

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

#normalizedPerformancePlots <- lapply(levels(processedData$benchmarkProblemName), normalizedPerformancePlot)

# print(normalizedPerformancePlots)

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

#print(overallScalingPlot + theme(legend.position = "bottom"))
#ggsave(file.path(outputDir, "allScalingPlot.pdf"), overallScalingPlot + theme(legend.position = "bottom"), width = 7.5, height = 2, units = "in")

# svg( file.path(outputDir, "allScalingPlot-legend.svg"), width = 7.5, height = 2 )
# print(overallScalingPlot + theme(legend.position = "bottom"))
# dev.off()
