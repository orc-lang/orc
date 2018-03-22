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
dataDir <- file.path(localExperimentDataDir, "20180321-a003")
#dataDir <- file.path(localExperimentDataDir, "20180203-a009")

loadData <- function(dataDir) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    mutate(benchmarkProblemName = factor(sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1]))) %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")"))) %>%
    mutate(nCPUs=48) %>%
    mutate(useGraft=as.logical(useGraft), sequentializeForce=as.logical(sequentializeForce))

  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "Black", "Black-Scholes", levels(data$benchmarkProblemName))
  levels(data$benchmarkProblemName) <- if_else(levels(data$benchmarkProblemName) == "KMeans", "K-Means", levels(data$benchmarkProblemName))

  prunedData <<- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "run", "useGraft", "sequentializeForce"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    filter(rtCompTime < cpuTime * 0.01)

  View(data)
  View(prunedData)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, useGraft, sequentializeForce) %>% bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    #group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
  processedData
}

if(!exists("processedData")) {
  processedData <- loadData(file.path(localExperimentDataDir, "20180321-a003")) %>% mutate(experiment = factor("only"))
  #processedData <- loadData(file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")) %>% mutate(experiment = "old")
  #processedData <- processedData %>%
  #  rbind(loadData(file.path(localExperimentDataDir, "20180311-a003")) %>% mutate(experiment = "new"))
  #processedData <- processedData %>% mutate(experiment = factor(experiment, ordered = T, levels = c("old", "new")))
  # processedData <- loadData(dataDir) %>% mutate(experiment = factor("only"))
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
Black-Scholes-scala-compute-for-tree (Orc),              Fine,        T,            Naïve,       TRUE
Black-Scholes-scala-compute-for-tree-opt (Orc),          Fine,        T,            Naïve,       F
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
SSSP-batched-partitioned (Orc),                          Coarse,      F,            Partition,   TRUE
Swaptions-naive-scala-sim (Orc),                         Coarse,      T,            Naïve,       F
Swaptions-naive-scala-subroutines-seq (Orc),             Fine,        T,            Naïve,       F
Swaptions-naive-scala-swaption (Orc),                    Coarse,      T,            Naïve,       TRUE
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
  addBaseline(elapsedTime_mean, c(isBaseline = T, useGraft = F, sequentializeForce = F, experiment = levels(processedData$experiment)[1]), baseline = elapsedTime_mean_problembaseline) %>%
  ungroup()

includedBenchmarks <- {
  txt <- "
  Black-Scholes-naive (Orc)
  Black-Scholes-partially-seq (Orc)
  Black-Scholes-par (Scala)
  Black-Scholes-partitioned-seq (Orc)
  Black-Scholes-scala-compute (Orc)
  Black-Scholes-scala-compute-for-tree (Orc)
  Black-Scholes-scala-compute-for-tree-opt (Orc)
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
  Swaptions-naive-scala-swaption (Orc)
  Swaptions-par-swaption (Scala)
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

# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

print(levels(processedData$benchmarkProblemName))

# Combined faceted plot for all benchmarks.

overallScalingPlot <- processedData %>%
  ggplot(aes(
    x = if(levels(experiment) == c("only")) benchmarkName else factor(paste(experiment, benchmarkName)),
    y = elapsedTime_mean_problembaseline / elapsedTime_mean,
    ymin = elapsedTime_mean_problembaseline / elapsedTime_mean_lowerBound,
    ymax = elapsedTime_mean_problembaseline / elapsedTime_mean_upperBound
    )) +
  labs(y = "Speed Up", x = "Number of Cores", shape = "Granularity", linetype = "Language", color = "Parallelism") +
  theme_minimal() +
  scale_color_manual(values = c("#555555", "#E69F00", "#56B4E9")) + # "#67a9cf", "#1c9099", "#016c59"
  geom_col_errorbar() +
  geom_hline(yintercept = 1, alpha = 0.4, color = "blue") +
  expand_limits(y = 0) +
  #scale_y_continuous(expand = c(0, 0.5)) +
  facet_grid(useGraft ~ sequentializeForce) +
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
  ) + theme(
    axis.text.x = element_text(size=8, angle=40, hjust=1),
    panel.grid.major.x = element_line(linetype = 0))

print(overallScalingPlot + theme(legend.position = "bottom"))
ggsave(file.path(outputDir, "allScalingPlot.pdf"), overallScalingPlot + theme(legend.position = "bottom"), width = 7.5, height = 2, units = "in")
