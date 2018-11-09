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
library(svglite)

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))
source(file.path(scriptDir, "porce", "utils.R"))


# dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")
#dataDir <- file.path(localExperimentDataDir, "20180730-a010")
dataDir <- file.path(localExperimentDataDir, "20181109-a001")
scalaDataDir <- file.path(localExperimentDataDir, "20181108-a001")

if(!exists("processedData")) {
  scalaData <- readMergedResultsTable(scalaDataDir, "benchmark-times", invalidate = F) %>%
    filter(language == "Scala")

  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    bind_rows(scalaData) %>%
    addBenchmarkProblemName()

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    filter(rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95, R = 1) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    #group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
}

clear__ <- function() {
  rm("processedData")
}

print(levels(processedData$benchmarkName))

processedData <- processedData %>%
  addBenchmarkMetadata() %>%
  group_by(benchmarkProblemName) %>%
  addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1, isBaseline = T), baseline = elapsedTime_mean_problembaseline) %>%
  mutate(elapsedTime_mean_problembaseline = replace_na(elapsedTime_mean_problembaseline, 60*10)) %>%
  group_by(benchmarkName) %>%
  addBaseline(elapsedTime_mean, c(nCPUs=24), baseline = elapsedTime_mean_selfbaseline) %>%
  ungroup()

#processedData <- processedData %>% filter(is.element(benchmarkName, includedBenchmarks))

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


## Seperate SELF Scaling Plots for each problem.

selfScalingPlot <- function(problemName) {
  p <- processedData %>% filter(benchmarkProblemName == problemName) %>% ggplot(aes(
    x = nCPUs,
    y = elapsedTime_mean_selfbaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_selfbaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_selfbaseline / elapsedTime_mean_upperBound,
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

selfScalingPlots <- lapply(levels(processedData$benchmarkProblemName), selfScalingPlot)

# Combined faceted plot for all benchmarks.

overallSelfScalingPlot <- processedData %>%
  ggplot(aes(
    x = nCPUs,
    y = elapsedTime_mean_selfbaseline / elapsedTime_mean,
    #ymin = elapsedTime_mean_selfbaseline / elapsedTime_mean_lowerBound,
    #ymax = elapsedTime_mean_selfbaseline / elapsedTime_mean_upperBound,
    color = implType,
    group = benchmarkName
    #linetype = factor(if_else((language == "Orc") & scalaCompute, "Orc+Scala", as.character(language)), levels = c("Orc+Scala", "Orc", "Scala"))
  )) +
  labs(y = "", x = "Number of Cores", color = "Language") +
  theme_minimal() +
  #scale_fill_brewer(palette="Set3") +
  #scale_color_brewer(palette="PuBuGn", direction = -1) +
  scale_color_manual(values = c("#555555", "#E69F00", "#56B4E9")) + # "#67a9cf", "#1c9099", "#016c59"
  # geom_point(data = processedData %>% filter(benchmarkProblemName != "Swaptions"), alpha = 0.5, shape = 4) +
  #geom_point(aes(shape = granularity), processedData, alpha = 0.7) +
  geom_line() +
  # geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
  scale_y_continuous(limits = c(0, NA)) +
  scale_x_continuous_breaks_from(breaks_from = processedData$nCPUs) +
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
  ) +
  ggtitle("Self Scaling for each version (normalized to 24-core)")

#print(overallSelfScalingPlot + theme(legend.position = "bottom"))

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
timeOutputDir <- file.path(dataDir, "times")
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

# Combined faceted plot for all benchmarks.

overallScalingPlot <- processedData %>%
  ggplot(aes(
    x = nCPUs,
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    #ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    #ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound,
    shape = implType,
    color = implType,
    group = benchmarkName,
    linetype = recode(factor(optimized), `TRUE` = "Opt.", `FALSE` = "Idiom.")
    )) +
  labs(y = "Speed Up", x = "", color = "Language", linetype = "") +
  theme_minimal() +
  #scale_fill_brewer(palette="Set3") +
  #scale_color_brewer(palette="PuBuGn", direction = -1) +
  scale_color_manual(name = "Language",
                     labels = c("Orc", "Orc+Scala", "Scala"),
                     values = c("#555555", "#E69F00", "#56B4E9")) + # "#67a9cf", "#1c9099", "#016c59"
  scale_shape_manual(name = "Language",
                     labels = c("Orc", "Orc+Scala", "Scala"),
                     values = c(0, 1, 2)) +
# geom_point(data = processedData %>% filter(benchmarkProblemName != "Swaptions"), alpha = 0.5, shape = 4) +
  # geom_point(aes(shape = granularity), processedData, alpha = 0.7) +
  #geom_line(aes(y = gcTime_mean), linetype = "dotted") +
  geom_line() +
  geom_point() +
  geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
  scale_y_continuous(limits = c(0, NA)) +
  scale_x_continuous(breaks = c(1, 12, 24), minor_breaks = c(6, 18)) +
  # scale_x_continuous_breaks_from(breaks_from = processedData$nCPUs) +
  facet_wrap(~benchmarkProblemName, scales = "free_y", nrow = 1) +
  theme(
    #legend.justification = c("right", "top"),
    #legend.box.just = "top",
    legend.margin = margin(-18, 0, 0, -30),
    legend.direction = "horizontal",
    #legend.box = "vertical",
    legend.box = "horizontal",
    legend.spacing = grid::unit(45, "points"),
    text = element_text(size=9),
    legend.text = element_text(size=8),
    strip.text = element_text(size=9, angle = 10)
  )

print(overallScalingPlot + theme(legend.position = "bottom"))

#print(overallScalingPlot + scale_y_log10() + theme(legend.position = "bottom"))

ggsave(file.path(outputDir, "allScalingPlot.svg"), device = "svg", overallScalingPlot + theme(legend.position = "bottom"), width = 10, height = 1.9, units = "in")

ggsave(file.path(outputDir, "allScalingPlot.pdf"), overallScalingPlot + theme(legend.position = "bottom"), width = 12, height = 2.1, units = "in")

# svg( file.path(outputDir, "allScalingPlot-legend.svg"), width = 7.5, height = 2 )
# print(overallScalingPlot + theme(legend.position = "bottom"))
# dev.off()

kable(processedData %>% select(benchmarkName, nCPUs, elapsedTime_mean) %>% spread(nCPUs, elapsedTime_mean), digits = 2)



# Benchmark vs. Implementation table

useNCPUs <- max(processedData$nCPUs)
useOptLevel <- max(processedData$optLevel, na.rm = T)

implTypes <- c("Orc -O0", paste0("Orc -O", useOptLevel), "Orc+Scala", "Scala")


longT <- processedData %>% filter(nCPUs == useNCPUs) %>%
  mutate(implType = factor(if_else(implType == "Orc", paste0("Orc -O", optLevel), as.character(implType)), levels = implTypes)) %>%
  mutate(implType = if_else(optimized, paste0(implType, " Opt"), as.character(implType)))

t <- longT %>%
  filter(optimized | language == "Scala") %>%
  select(benchmarkProblemName, implType, elapsedTime_mean) %>% spread(implType, elapsedTime_mean)

write.csv(t, file = file.path(timeOutputDir, "mean_elapsed_time.csv"), row.names = F)

print(t)
print(kable(t, "latex"))

filledInData <- processedData %>%
  full_join(processedData %>%
              filter(optLevel == 3, implType == "Orc") %>%
              transmute(benchmarkProblemName, benchmarkName, optLevel = 0, elapsedTime_mean = 8 * 60),
            by = c("benchmarkProblemName", "benchmarkName", "optLevel")) %>%
  mutate(elapsedTime_mean = if_else(is.na(elapsedTime_mean.x), elapsedTime_mean.y, elapsedTime_mean.x), elapsedTime_mean.x = NULL, elapsedTime_mean.y = NULL)

times2 <-
  full_join(filledInData,
            processedData %>%
              filter(implType == "Orc", optLevel == 3) %>%
              transmute(benchmarkProblemName, orc3Time = elapsedTime_mean)) %>%
  full_join(processedData %>%
              filter(implType == "Scala") %>%
              transmute(benchmarkProblemName, scalaTime = elapsedTime_mean))
#filter(implType == "Orc") %>%
#mutate(normalizedTime = elapsedTime_mean / orc3Time)

print(
  list(
    O0_to_O3 =
      geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / orc3Time) %>% filter(optLevel == 0))$normalizedTime),
    Orc_to_total_Scala_with_timeouts =
      geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / if_else(is.na(scalaTime), 8 * 60, scalaTime)) %>% filter(implType == "Orc", optLevel == 3))$normalizedTime),
    Orc_Scala_to_total_Scala_with_timeouts =
      geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / if_else(is.na(scalaTime), 8 * 60, scalaTime)) %>% filter(implType == "Orc+Scala", optLevel == 3))$normalizedTime),
    Orc_to_total_Scala =
      geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / scalaTime) %>% filter(implType == "Orc", optLevel == 3, normalizedTime > 1))$normalizedTime),
    Orc_Scala_to_total_Scala =
      geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / scalaTime) %>% filter(implType == "Orc+Scala", optLevel == 3, normalizedTime > 1))$normalizedTime)
  )
)

