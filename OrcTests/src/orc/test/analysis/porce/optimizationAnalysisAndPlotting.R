# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(tikzDevice)
library(knitr)
library(tidyr)

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171024-a003")
dataDir <- file.path(localExperimentDataDir, "20171109-a004")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])))

  prunedData <- data %>% dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs"), rep, elapsedTime, 5, 50, 120)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, benchmarkName, nCPUs, step) %>% bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime"), mean) %>%
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

sampleCountData <- processedData %>% transmute(benchmarkName = benchmarkName, config = paste0(step, " on ", nCPUs), nSamples = nSamples) %>% spread(config, nSamples)

# Sample count table

sampleCountTable <- function(format) {
  kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

processedData <- processedData %>% filter(benchmarkName != "BigSort-naive")
processedData <- full_join(processedData, with(processedData, expand.grid(benchmarkName = unique(benchmarkName), nCPUs = unique(nCPUs), step = unique(step))))

levels(processedData$benchmarkName) <- c("BigSort-naive", "Black-Scholes", "Canneal", "k-Means", "Mandelbrot", "SSSP", "Swaptions")

p <- processedData %>% ggplot(aes(
  x = factor(nCPUs),
  y = elapsedTime_mean,
  ymin = elapsedTime_mean_lowerBound,
  ymax = elapsedTime_mean_upperBound,
  fill = factor(step))) +
  labs(y = "Elapsed Execution Time (s)", x = "Number of CPUs", fill = "Development\nStep") +
  theme_minimal() +
  facet_wrap(~benchmarkName, scales = "free_y")

fullPerformancePlot <- p + geom_col_errorbar()
  # geom_text(aes(label = format(elapsedTime_mean, digits = 2, nsmall=0),
  #               y = pmax(pmin(elapsedTime_mean + (0.01 * max(elapsedTime_mean)), max(elapsedTime_mean) * 0.9), 0)),
  #           position = position_dodge(0.9), vjust = 0)

# print(fullPerformancePlot)


# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

# for (problem in levels(processedData$benchmarkProblemName)) {
#   ggsave(file.path(outputDir, paste0(problem, "-scaling.pdf")), scalingPlot(problem), width = 7.5, height = 6)
#   ggsave(file.path(outputDir, paste0(problem, "-normPerformance.pdf")), normalizedPerformancePlot(problem), width = 7.5, height = 8)
# }

tikz(file = ".tex", width = 3, height = 2)

fullPerformancePlot

dev.off()

ggsave(file.path(outputDir, "developmentProgressPlot.pdf"), fullPerformancePlot, width = 7.5, height = 8)

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")
capture.output(sampleCountTable("latex"), file = file.path(outputDir, "usedSampleCounts.tex"), type = "output")


