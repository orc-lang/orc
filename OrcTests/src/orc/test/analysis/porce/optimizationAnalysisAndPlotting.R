# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

#library(knitr)
#library(tikzDevice)
library(tidyr)
library(cowplot)

#theme_set(theme_gray())

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))


dataDir <- file.path(experimentDataDir, "PorcE", "development", "20171111-a003")
#dataDir <- file.path(localExperimentDataDir, "20171111-a003")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times") %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])))

  prunedData <- data %>% dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "step"), rep, elapsedTime, 2, 60, 180, maxRemaining = 10)

  processedData <- prunedData %>%
    group_by(benchmarkName, nCPUs, step) %>% bootstrapStatistics(c("elapsedTime", "cpuTime"), mean) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_lowerBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_upperBound) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(step=0), baseline = elapsedTime_mean_none) %>%
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
#plotAllData(data)

sampleCountData <- processedData %>% transmute(benchmarkName = benchmarkName, config = step, nSamples = nSamples) %>% spread(config, nSamples)

# Sample count table

sampleCountTable <- function(format) {
  #kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
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
  "Static Peephole Opt.",
  "Static Inlining",
  "Static Force Elim.",
  "Static Future Elim.",
  "Dynamic PIC",
  "Dynamic Peephole Opt.",
  "Dynamic Task Inlining"
  # "Limited Prec. Arith."
))

processedData <- left_join(processedData, processedData %>% group_by(benchmarkName) %>% summarise(elapsedTime_mean_max = max(elapsedTime_mean, na.rm = T))) %>% ungroup()

plotSingleBenchmark <- function(benchmark, labels = TRUE) {
  d <- processedData %>% filter(benchmarkName == benchmark)
  ymax <- max(d[2:length(d),]$elapsedTime_mean / d[2:length(d),]$elapsedTime_mean_none, na.rm = T) * 1.2
  p <- d %>% ggplot(aes(
    x = step,
    fill = step,
    y = elapsedTime_mean / elapsedTime_mean_none)) +
    labs(y = if (labels) "Elapsed Time (normalized)" else NULL, x = NULL) +
    theme_minimal() +
    scale_y_continuous(labels = scales::percent) +
    facet_wrap(~benchmarkName, scales = "free_y") +
    theme(
      text = element_text(size = 7),
      axis.text.y = element_text(size = 6),
      axis.text.x = if (labels)  element_text(angle=90, hjust=1, vjust = 0.4, size = 5, color = "black") else element_blank(),
      panel.grid.major.x = element_line(linetype = 0)) +
    coord_cartesian(ylim = c(0, 1.1 * ymax)) +
    scale_fill_brewer(palette = "Dark2")

  p +
    geom_col(show.legend = F) +
    geom_text(aes(label = if_else(is.na(elapsedTime_mean), "Timeout", ""), y = elapsedTime_mean_max * 0.95),
              position = position_dodge(0.9), vjust = "center", hjust = "right", angle = 90) +
    geom_text(aes(label = if_else(elapsedTime_mean / elapsedTime_mean_none > ymax, "100%", ""), # format(elapsedTime_mean / elapsedTime_mean_none, digits = 2, nsmall=0)
                  y = pmax(pmin(elapsedTime_mean / elapsedTime_mean_none + (0.01 * ymax), ymax * 0.8), 0)
                  ),
              position = position_dodge(0.9), vjust = "middle", hjust = "left", angle = 90, size = 1.8)
}

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

#tikz(file = file.path(outputDir, "optimizationProgress_gg.tex"), width = 6, height = 4)
#print(fullPerformancePlot)
#dev.off()

# ggsave(file.path(outputDir, "optimizationProgress_gg.pdf"), fullPerformancePlot, width = 6, height = 4)

#capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")
#capture.output(sampleCountTable("latex"), file = file.path(outputDir, "usedSampleCounts.tex"), type = "output")

#basePlots <- lapply(c("Black-scholes", "Canneal", "k-Means", "SSSP (BFS)", "Swaptions"), function(v) plotSingleBenchmark(v, F))
#alignedPlots <- align_plots(plotlist = basePlots, align = "vh")
#plot_grid(plotlist = alignedPlots)

#tikz(file = file.path(outputDir, "optimizationProgress.tex"), width = 6*0.54, height = 4*0.54)
p <- plot_grid(
  plot_grid(plotSingleBenchmark("Black-scholes"), NULL, ncol = 1, axis = "lr", rel_heights = c(1, 0.18)),
  plot_grid(plotSingleBenchmark("Canneal", F), plotSingleBenchmark("Swaptions", F), ncol = 1, align = "v"),
  plot_grid(plotSingleBenchmark("SSSP (BFS)", F), plotSingleBenchmark("k-Means", F), ncol = 1, align = "v"),
  nrow = 1, rel_widths = c(1.12, 1, 1))
ggsave(p, file = file.path(outputDir, "optimizationProgress.pdf"), width = 6*0.54, height = 4*0.54, units = "in")
#dev.off()

# ggsave(file.path(outputDir, "optimizationProgress.pdf"), plot_grid(
#   plot_grid(plotSingleBenchmark("Black-scholes"), NULL, ncol = 1, axis = "lr", rel_heights = c(1, 0.472)),
#   plot_grid(plotSingleBenchmark("Canneal", F), plotSingleBenchmark("Swaptions", F), ncol = 1, align = "h"),
#   plot_grid(plotSingleBenchmark("SSSP (BFS)", F), plotSingleBenchmark("k-Means", F), ncol = 1, align = "h"),
#   nrow = 1, rel_widths = c(1.04, 1, 1)), width = 6, height = 4)


print(processedData %>% mutate(measuredTime = elapsedTime_mean * nSamples) %>% select(benchmarkName, step, measuredTime, nSamples))



p <- processedData %>% ggplot(aes(
  x = step,
  fill = step,
  y = cpuUtilization, # if_else(is.na(elapsedTime_mean), elapsedTime_mean_max * 1, elapsedTime_mean),
  ymin = cpuUtilization_lowerBound,
  ymax = cpuUtilization_upperBound)) +
  labs(y = "CPU Utilization", x = NULL) +
  theme_minimal() +
  facet_wrap(~benchmarkName, scales = "free_y") +
  theme(axis.text.x = element_text(size=8, angle=40, hjust=1),
        panel.grid.major.x = element_line(linetype = 0)) +
  fillPalette
#scale_fill_brewer(palette = "Dark2")
#coord_cartesian(ylim = c(0, max(processedData$elapsedTime_mean, na.rm = T)))

fullPerformancePlot <- p +
  geom_col_errorbar(show.legend = F)

print(fullPerformancePlot)
