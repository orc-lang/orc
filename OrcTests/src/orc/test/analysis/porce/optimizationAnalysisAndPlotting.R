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
dataDir <- file.path(localExperimentDataDir, "20171111-a003")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])))

  prunedData <- data %>% dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "step"), rep, elapsedTime, 2, 60, 180)

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
  d <- data %>% group_by(benchmarkName, nCPUs, step) %>% mutate(norm = elapsedTime / min(elapsedTime), startTime = cumsum(elapsedTime))
  vis <- list(geom_point(alpha = 0.8, position = position_jitter()), colorPalette)
  p <- d %>% ggplot(aes(x = rep, y = norm, color = benchmarkName)) + vis
  print(p)
  p <- d %>% ggplot(aes(x = startTime, y = norm, color = benchmarkName)) + vis
  print(p)
}

# Turn on to view data and evaluate the number warm up iterations.
plotAllData(data)

sampleCountData <- processedData %>% transmute(benchmarkName = benchmarkName, config = step, nSamples = nSamples) %>% spread(config, nSamples)

# Sample count table

sampleCountTable <- function(format) {
  kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

processedData <- processedData %>% filter(benchmarkName != "BigSort-naive")
processedData <- full_join(processedData, with(processedData, expand.grid(benchmarkName = unique(benchmarkName), nCPUs = unique(nCPUs), step = unique(step))))

levels(processedData$benchmarkName) <- vapply(levels(processedData$benchmarkName), function(v) {
  switch(sub("([:alpha:]*)-.*", "\\1", v),
         "Black" = "Black-scholes",
         "Canneal" = "Canneal",
         "KMeans" = "k-Means",
         "SSSP" = "SSSP (BFS)",
         "Swaptions" = "Swaptions",
         v)
}, "")

processedData$step <- factor(processedData$step, labels = c(
  "None",
  "Static Peephole",
  "Static Inlining",
  "Static Force Elim.",
  "Static Future Elim.",
  "Dynamic Task Inlining",
  "Dynamic PIC",
  "Dynamic Peephole"
  # "Limited Prec. Arith."
))

processedData <- left_join(processedData, processedData %>% group_by(benchmarkName) %>% summarise(elapsedTime_mean_max = max(elapsedTime_mean, na.rm = T)))

p <- processedData %>% ggplot(aes(
  x = step,
  fill = step,
  y = elapsedTime_mean, # if_else(is.na(elapsedTime_mean), elapsedTime_mean_max * 1, elapsedTime_mean),
  ymin = elapsedTime_mean_lowerBound,
  ymax = elapsedTime_mean_upperBound)) +
  labs(y = "Elapsed Time (seconds)", x = NULL) +
  theme_minimal() +
  facet_wrap(~benchmarkName, scales = "free_y") +
  theme(axis.text.x = element_text(size=8, angle=40, hjust=1),
        panel.grid.major.x = element_line(linetype = 0)) +
  fillPalette
  #scale_fill_brewer(palette = "Dark2")
  #coord_cartesian(ylim = c(0, max(processedData$elapsedTime_mean, na.rm = T)))

fullPerformancePlot <- p +
  geom_col(show.legend = F) +
  geom_text(aes(label = if_else(is.na(elapsedTime_mean), "Timeout", ""), y = elapsedTime_mean_max * 0.95),
            position = position_dodge(0.9), vjust = "center", hjust = "right", angle = 90)

fullPerformancePlot

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

tikz(file = file.path(outputDir, "optimizationProgress.tex"), width = 6, height = 4)
print(fullPerformancePlot)
dev.off()

print(fullPerformancePlot)

ggsave(file.path(outputDir, "optimizationProgress.pdf"), fullPerformancePlot, width = 6.5, height = 4)

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")
capture.output(sampleCountTable("latex"), file = file.path(outputDir, "usedSampleCounts.tex"), type = "output")


