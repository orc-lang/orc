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
dataDir <- file.path(localExperimentDataDir, "20171113-a007")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times") %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])),
           allowSpawnInlining = as.logical(allowSpawnInlining),
           allowAllSpawnInlining = as.logical(allowAllSpawnInlining))

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "timeLimit", "allowSpawnInlining", "allowAllSpawnInlining"), rep, elapsedTime, 5, 50, 120)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, timeLimit, allowSpawnInlining, allowAllSpawnInlining) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime"), mean) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_lowerBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_upperBound) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(timeLimit=0.0), baseline = elapsedTime_mean_selfbaseline) %>%
    ungroup() %>%
    mutate(configuration = paste0(
      if_else(allowSpawnInlining & allowAllSpawnInlining, "All",
              if_else(!allowSpawnInlining & !allowAllSpawnInlining, "None", "Old")),
      ", ", timeLimit*1000, "us")) %>%
    mutate(configuration = factor(configuration, levels = unique(configuration[order(allowSpawnInlining, allowAllSpawnInlining, timeLimit)])))
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
  x = factor(configuration),
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
    x = factor(configuration),
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
    x = factor(configuration),
    y = elapsedTime_mean_selfbaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_selfbaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_selfbaseline / elapsedTime_mean_upperBound,
    fill = benchmarkName)) +
    labs(y = "Speed-up w.r.t. no inlining (large bar)\nCPU Utilization Ratio (white outline bar)", x = "", fill = "Benchmark",
         caption = "All = allow inlining any spawn, None = disallow spawn inlining,\nOld = allow spawn inlining except for recursive calls") +
    ggtitle(problemName) +
    theme_minimal() +
    facet_wrap(allowSpawnInlining ~ allowAllSpawnInlining, scales = "free_x", nrow = 1, labeller = function(.) "") +
    theme(axis.text.x = element_text(angle=80, hjust=1)) +
    scale_fill_brewer(palette="Dark2", guide = guide_legend(
                          direction = "horizontal",
                          title.position = "top",
                          #label.position = "bottom",
                          label.hjust = -0.5,
                          label.vjust = -0.5,
                          label.theme = element_text(angle = 90)
                        ))


  scalingPlot <- p + geom_col_errorbar() +
    geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
    geom_col(aes(y = cpuUtilization / nCPUs,
                 ymin = cpuUtilization_lowerBound / nCPUs,
                 ymax = cpuUtilization_upperBound / nCPUs
    ), position = position_dodge(0.9), alpha = 0.5, color = "white", width = 0.2) +
    geom_errorbar(aes(y = cpuUtilization / nCPUs,
                      ymin = cpuUtilization_lowerBound / nCPUs,
                      ymax = cpuUtilization_upperBound / nCPUs
    ), position = position_dodge(0.9), alpha = 0.3, width = 0.2)

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

selfScalingPlot <- p + geom_line(aes(x = factor(timeLimit), color = benchmarkName))
#fullPerformancePlot <- p + geom_col_errorbar(aes(x = factor(nCPUs)))


outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

for (problem in levels(processedData$benchmarkProblemName)) {
  ggsave(file.path(outputDir, paste0(problem, "-sensitivity.pdf")), scalingPlot(problem), width = 7, height = 5)
}
