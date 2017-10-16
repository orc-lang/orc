# plotElapsedTimeDistribScaleTest.R -- R script to plot elapsed time results from DistribScaleTest
# Project OrcTests
#
# Created by jthywiss on Oct 13, 2017.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(dplyr)
library(ggplot2)

source("analysis.R")

allRepetitionTimes <- read.csv("repetition-times.csv")
names(allRepetitionTimes) <- c("program", "repeatRead", "numInputFiles", "dOrcNumRuntimes", "repetitionNumber", "elapsedTime")

warmRepetitionTimes <- allRepetitionTimes[allRepetitionTimes$repetitionNumber >= 3,]

baselineTimeSummary <- warmRepetitionTimes[warmRepetitionTimes$dOrcNumRuntimes == 1 | is.na(warmRepetitionTimes$dOrcNumRuntimes),] %>%
  group_by(program, repeatRead, numInputFiles, dOrcNumRuntimes) %>%
  summarise(nElapsedTime = length(elapsedTime), meanElapsedTime = mean(elapsedTime), sdElapsedTime = sd(elapsedTime), seElapsedTime = sdElapsedTime / sqrt(nElapsedTime))


# Plot baseline elapsed times

for (currProgram in unique(baselineTimeSummary$program)) {
  ggplot(baselineTimeSummary[baselineTimeSummary$program == currProgram,], aes(x = factor(numInputFiles), y = meanElapsedTime, group = 1)) +
  geom_point() +
  stat_summary(fun.y = mean, geom = "line") +
  ggtitle(currProgram) +
  xlab("Number of files read") +
  labs(fill = "Cluster size [Number of d-Orc runtimes]") +
  scale_y_continuous(name = "Elapsed time (s)", labels = function(n){format(n / 1000000, scientific = FALSE)}) +
  geom_errorbar(aes(ymax = meanElapsedTime + seElapsedTime, ymin = meanElapsedTime - seElapsedTime), width = 0.2, alpha = 0.35, position = "dodge") +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1))

  ggsave(paste0("elapsedTime_", currProgram, ".pdf"), width = 7, height = 7)
}


# Plot speedups

elapsedTimeSummary <- warmRepetitionTimes[!is.na(warmRepetitionTimes$dOrcNumRuntimes),] %>%
  group_by(program, repeatRead, numInputFiles, dOrcNumRuntimes) %>%
  summarise(nElapsedTime = length(elapsedTime), meanElapsedTime = mean(elapsedTime), sdElapsedTime = sd(elapsedTime), seElapsedTime = sdElapsedTime / sqrt(nElapsedTime)) %>%
  addBaseline(meanElapsedTime, c(dOrcNumRuntimes = 1)) %>%
  mutate(speedup = meanElapsedTime_baseline / meanElapsedTime, speedupSeMax = meanElapsedTime_baseline / (meanElapsedTime + seElapsedTime), speedupSeMin = meanElapsedTime_baseline / (meanElapsedTime - seElapsedTime))

options(tibble.print_max = Inf, tibble.width = Inf)

for (currProgram in unique(elapsedTimeSummary$program[elapsedTimeSummary$dOrcNumRuntimes > 1 & !is.na(elapsedTimeSummary$dOrcNumRuntimes)])) {

  ggplot(elapsedTimeSummary[elapsedTimeSummary$program == currProgram & elapsedTimeSummary$dOrcNumRuntimes > 1,], aes(x = factor(numInputFiles), y = speedup, group = factor(dOrcNumRuntimes), colour = factor(dOrcNumRuntimes), shape = factor(dOrcNumRuntimes))) +
  geom_line() +
  geom_point(size = 2) +
  ggtitle(currProgram) +
  xlab("Number of files read") +
  labs(colour = "Cluster size [Number of d-Orc runtimes]", shape = "Cluster size [Number of d-Orc runtimes]") +
  scale_y_continuous(name = "Speed-up factor over cluster size 1", labels = function(n){format(n, scientific = FALSE)}) +
  geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1))

  ggsave(paste0("speedup_", currProgram, ".pdf"), width = 7, height = 7)
}

