# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

scriptDir <- dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))

plotAllData <- function(data) {
  p <- data %>% group_by(benchmarkName, nCPUs) %>% mutate(norm = elapsedTime / min(elapsedTime)) %>% ungroup() %>%
    ggplot(aes(x = rep, y = norm, color = benchmarkName)) + geom_point(alpha = 0.4)
  print(p)
}

dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171015-a001-merged")
#dataDir <- file.path(localExperimentDataDir, "20171017-a005")

data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = F)

# Turn on to view data and evaluate the number warm up iterations.
plotAllData(data)

prunedData <- data %>% dropWarmupRepetitions(50)

processedData <- prunedData %>%
  group_by(benchmarkName, nCPUs) %>% bootstrapStatistics(c("elapsedTime"), mean) %>%
  group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1))

p <- processedData %>% ggplot(aes(
    y = elapsedTime_mean_baseline / elapsedTime_mean,
    ymin = elapsedTime_mean_baseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_baseline / elapsedTime_mean_upperBound,
    fill = benchmarkName)) +
  labs(y = "Speed up", x = "Number of CPUs", color = "Benchmark", fill = "Benchmark") +
  theme_minimal()

scalingPlot <- p + geom_line(aes(x = nCPUs, color = benchmarkName)) + scale_x_continuous_breaks_from(breaks_from = processedData$nCPUs)
fullPerformancePlot <- p + geom_col_errorbar(aes(x = factor(nCPUs)))

print(scalingPlot)
print(fullPerformancePlot)

#ggsave(file.path(dataDir, "scalingPlot.pdf"), scalingPlot, width = 7.5, height = 6)
#ggsave(file.path(dataDir, "fullPlot.pdf"), fullPerformancePlot, width = 7.5, height = 8)


