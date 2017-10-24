# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

scriptDir <- getwd() #dirname(dirname(sys.frame(1)$ofile))

experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))

plotAllData <- function(data) {
  p <- data %>% group_by(benchmarkName) %>% mutate(norm = elapsedTime / min(elapsedTime)) %>% ungroup() %>%
    ggplot(aes(x = rep, y = norm, color = benchmarkName)) + geom_point(alpha = 0.4)
  print(p)
}

#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171015-a001-merged")
dataDir <- file.path(localExperimentDataDir, "20171019-a006")

data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = F)

data <- data %>% filter(language == "Orc") #%>% filter(benchmarkName %in% c("Black-Scholes-naive", "Canneal-naive"))

# Turn on to view data and evaluate the number warm up iterations.
#plotAllData(data)

processedData <-
  data %>% group_by(benchmarkName) %>% mutate(elapsedTime_normalized = elapsedTime / min(elapsedTime)) %>%
  group_by(benchmarkName, TruffleCompilationThreshold) %>% mutate(elapsedTime_cum = cumsum(elapsedTime))

summarizedData <-
  data %>% group_by(benchmarkName, TruffleCompilationThreshold) %>% dropWarmupRepetitions(5) %>% bootstrapStatistics(c("elapsedTime"), mean)

print(summarizedData %>% ggplot(aes(
  y = elapsedTime_mean,
  ymin = elapsedTime_mean_lowerBound,
  ymax = elapsedTime_mean_upperBound,
  fill = factor(TruffleCompilationThreshold))) +
  theme_minimal() +
    geom_col_errorbar(aes(x = factor(benchmarkName)))
  )

p <- processedData %>% ggplot(aes(
    y = elapsedTime,
    color = factor(TruffleCompilationThreshold)
    )) +
  facet_wrap(~benchmarkName, scales = "free", ncol = 1) +
  ylim(c(0, NA)) +
  theme_minimal()

print(p + geom_line(aes(x = rep)))
print(p + geom_line(aes(x = elapsedTime_cum)))

#ggsave(file.path(dataDir, "scalingPlot.pdf"), scalingPlot, width = 7.5, height = 6)
#ggsave(file.path(dataDir, "fullPlot.pdf"), fullPerformancePlot, width = 7.5, height = 8)


