# plotElapsedTimeWordCount.R -- R script to plot elapsed time results from WordCount DistribScaleTest
# Project OrcTests
#
# Created by jthywiss on Oct 13, 2017.
#
# Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(dplyr, warn.conflicts = FALSE)
library(ggplot2)

source("analysis.R")

options(echo = TRUE)

runNumber <- commandArgs(trailingOnly = TRUE)[1]
warmupReps <- 9

allRepetitionTimes <- read.csv("repetition-times.csv")
names(allRepetitionTimes) <- c("program", "inputFileSize", "repeatRead", "numInputFiles", "dOrcNumRuntimes", "repetitionNumber", "elapsedTime", "cpuTime")

warmRepetitionTimes <- allRepetitionTimes[allRepetitionTimes$repetitionNumber >= (warmupReps + 1),]

baselineTimeSummary <- warmRepetitionTimes[warmRepetitionTimes$dOrcNumRuntimes == 1 | is.na(warmRepetitionTimes$dOrcNumRuntimes),] %>%
  group_by(program, inputFileSize, repeatRead, numInputFiles, dOrcNumRuntimes) %>%
  summarise(nElapsedTime = length(elapsedTime), meanElapsedTime = mean(elapsedTime), sdElapsedTime = sd(elapsedTime), seElapsedTime = sdElapsedTime / sqrt(nElapsedTime)) %>%
  rowwise() %>% mutate(sizeInput = numInputFiles * as.numeric(inputFileSize) / 1.0e6)

# Plot baseline elapsed times

for (currProgram in unique(baselineTimeSummary$program)) {

  # Full-size plot

  ggplot(baselineTimeSummary[baselineTimeSummary$program == currProgram,], aes(x = sizeInput, y = meanElapsedTime, group = 1)) +
  geom_point() +
  stat_summary(fun.y = mean, geom = "line") +
  ggtitle(paste(currProgram, "Run", runNumber)) +
  xlab("Input size (MB)") +
  labs(fill = "Cluster size [Number of d-Orc runtimes]") +
  scale_y_continuous(name = "Elapsed time (s)", labels = function(n){format(n / 1000000, scientific = FALSE)}) +
  expand_limits(y = 0.0) +
  geom_errorbar(aes(ymax = meanElapsedTime + seElapsedTime, ymin = meanElapsedTime - seElapsedTime), width = 0.2, alpha = 0.35, position = "dodge") +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1))

  ggsave(paste0("elapsedTime_v_inputSize_", currProgram, ".pdf"), width = 7, height = 7)

  # Small-size plot

  ggplot(baselineTimeSummary[baselineTimeSummary$program == currProgram,], aes(x = sizeInput, y = meanElapsedTime, group = 1)) +
  geom_point(size = 7) +
  stat_summary(fun.y = mean, geom = "line") +
  xlab("Input size (MB)") +
  labs(fill = "Cluster size [Number of runtimes]") +
  scale_y_continuous(name = "Elapsed time (s)", labels = function(n){format(n / 1000000, scientific = FALSE)}) +
  expand_limits(y = 0.0) +
  # geom_errorbar(aes(ymax = meanElapsedTime + seElapsedTime, ymin = meanElapsedTime - seElapsedTime), width = 0.2, alpha = 0.35, position = "dodge") +
  theme_minimal() +
  theme(legend.position = "none", axis.text=element_text(size=24), axis.title=element_text(size=28))

  ggsave(paste0("elapsedTime_v_inputSize_", currProgram, "_sm.pdf"), width = 7, height = 7)

}


# Plot speedups

elapsedTimeSummary <- warmRepetitionTimes[!is.na(warmRepetitionTimes$dOrcNumRuntimes),] %>%
  group_by(program, inputFileSize, repeatRead, numInputFiles, dOrcNumRuntimes) %>%
  summarise(nElapsedTime = length(elapsedTime), meanElapsedTime = mean(elapsedTime), sdElapsedTime = sd(elapsedTime), seElapsedTime = sdElapsedTime / sqrt(nElapsedTime)) %>%
  addBaseline(meanElapsedTime, c(dOrcNumRuntimes = 1)) %>%
  mutate(speedup = meanElapsedTime_baseline / meanElapsedTime) %>%
  rowwise() %>% mutate(sizeInput = numInputFiles * as.numeric(inputFileSize) / 1.0e6)

options(tibble.print_max = Inf, tibble.width = Inf)

for (currProgram in unique(elapsedTimeSummary$program[elapsedTimeSummary$dOrcNumRuntimes > 1 & !is.na(elapsedTimeSummary$dOrcNumRuntimes)])) {

  # Full-size plot

  ggplot(elapsedTimeSummary[elapsedTimeSummary$program == currProgram & elapsedTimeSummary$dOrcNumRuntimes > 1,], aes(x = dOrcNumRuntimes, y = speedup, group = factor(sizeInput), colour = factor(sizeInput), shape = factor(sizeInput))) +
  geom_line() +
  geom_point(size = 3) +
  ggtitle(paste(currProgram, "Run", runNumber)) +
  xlab("Cluster size [Number of d-Orc runtimes]") +
  labs(colour = "Input size (MB)", shape = "Input size (MB)") +
  scale_y_continuous(name = "Speed-up factor over cluster size 1", labels = function(n){format(n, scientific = FALSE)}) +
  expand_limits(x = 1, y = 1.0) +
  # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1))

  ggsave(paste0("speedup_v_numRuntime_", currProgram, ".pdf"), width = 7, height = 7)

  # Small-size plot

  ggplot(elapsedTimeSummary[elapsedTimeSummary$program == currProgram & elapsedTimeSummary$dOrcNumRuntimes > 1,], aes(x = dOrcNumRuntimes, y = speedup, group = factor(sprintf("%0.1f", sizeInput)), colour = factor(sprintf("%0.1f", sizeInput)), shape = factor(sprintf("%0.1f", sizeInput)))) +
  geom_line(size = 2) +
  geom_point(size = 7) +
  xlab("Cluster size [Number of runtimes]") +
  labs(colour = "Input size (MB)", shape = "Input size (MB)") +
  scale_y_continuous(name = "Speed-up factor over cluster size 1", labels = function(n){format(n, scientific = FALSE)}) +
  expand_limits(x = 1, y = 1.0) +
  # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1), legend.title=element_text(size=20), legend.text=element_text(size=20), axis.text=element_text(size=24), axis.title=element_text(size=28))

  ggsave(paste0("speedup_v_numRuntime_", currProgram, "_sm.pdf"), width = 7, height = 7)

}

for (currProgram in unique(elapsedTimeSummary$program[elapsedTimeSummary$dOrcNumRuntimes > 1 & !is.na(elapsedTimeSummary$dOrcNumRuntimes)])) {

  # Full-size plot

  ggplot(elapsedTimeSummary[elapsedTimeSummary$program == currProgram & elapsedTimeSummary$dOrcNumRuntimes > 1,], aes(x = sizeInput, y = speedup, group = factor(dOrcNumRuntimes), colour = factor(dOrcNumRuntimes), shape = factor(dOrcNumRuntimes))) +
  geom_line() +
  geom_point(size = 3) +
  ggtitle(paste(currProgram, "Run", runNumber)) +
  xlab("Input size (MB)") +
  labs(colour = "Cluster size [Number of d-Orc runtimes]", shape = "Cluster size [Number of d-Orc runtimes]") +
  scale_y_continuous(name = "Speed-up factor over cluster size 1", labels = function(n){format(n, scientific = FALSE)}) +
  expand_limits(x = 0, y = 1.0) +
  # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1))

  ggsave(paste0("speedup_v_inputSize_", currProgram, ".pdf"), width = 7, height = 7)

  # Small-size plot

  ggplot(elapsedTimeSummary[elapsedTimeSummary$program == currProgram & elapsedTimeSummary$dOrcNumRuntimes > 1,], aes(x = sizeInput, y = speedup, group = factor(dOrcNumRuntimes), colour = factor(dOrcNumRuntimes), shape = factor(dOrcNumRuntimes))) +
  geom_line(size = 2) +
  geom_point(size = 7) +
  xlab("Input size (MB)") +
  labs(colour = "Cluster size [Number of runtimes]", shape = "Cluster size [Number of runtimes]") +
  scale_y_continuous(name = "Speed-up factor over cluster size 1", labels = function(n){format(n, scientific = FALSE)}) +
  expand_limits(x = 0, y = 1.0) +
  # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
  theme_minimal() +
  theme(legend.justification = c(0, 1), legend.position = c(0, 1),  legend.title=element_text(size=20), legend.text=element_text(size=20), axis.text=element_text(size=24), axis.title=element_text(size=28))

  ggsave(paste0("speedup_v_inputSize_", currProgram, "_sm.pdf"), width = 7, height = 7)

}

# Small version of max-file case only, for printing at a small size

maxNumFiles <- max(elapsedTimeSummary$numInputFiles)

for (currProgram in unique(elapsedTimeSummary$program[elapsedTimeSummary$dOrcNumRuntimes > 1 & !is.na(elapsedTimeSummary$dOrcNumRuntimes)])) {

  ggplot(elapsedTimeSummary[elapsedTimeSummary$program == currProgram & elapsedTimeSummary$dOrcNumRuntimes > 1 & elapsedTimeSummary$numInputFiles == maxNumFiles,], aes(x = dOrcNumRuntimes, y = speedup, group = factor(sprintf("%0.1f", sizeInput)), colour = factor(sprintf("%0.1f", sizeInput)), shape = factor(sprintf("%0.1f", sizeInput)))) +
  geom_line(size = 2) +
  geom_point(size = 7) +
  xlab("Cluster size [Number of runtimes]") +
  labs(colour = "Input size (MB)", shape = "Input size (MB)") +
  scale_y_continuous(name = "Speed-up factor over cluster size 1", labels = function(n){format(n, scientific = FALSE)}) +
  expand_limits(x = 1, y = 1.0) +
  # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
  theme_minimal() +
  theme(legend.position = "none", axis.text=element_text(size=24), axis.title=element_text(size=28))

  ggsave(paste0("speedup_v_numRuntime_", currProgram, "_", maxNumFiles, "_sm.pdf"), width = 7, height = 7)
}

# Small version of baseline elapsed times for cluster size 1 case only, for printing at a small size

baselineTimeSummaryRc <- baselineTimeSummary
baselineTimeSummaryRc$program <- recode(baselineTimeSummaryRc$program, "WordCount.java"="Java 1 thr", "wordcount-pure-orc.orc"="pure dOrc", "wordcount-mixed-orc-java.orc"="Java+dOrc", "WordCountHadoop.java"="Hadoop")

{
  ggplot(baselineTimeSummaryRc[(is.na(baselineTimeSummaryRc$dOrcNumRuntimes) | baselineTimeSummaryRc$dOrcNumRuntimes == 1) & baselineTimeSummaryRc$numInputFiles == maxNumFiles,], aes(x = program, y = meanElapsedTime, group = 1, colour = program, fill = program)) +
  geom_col() +
  xlab("Program variant") +
  scale_y_continuous(name = "Elapsed time (s)", labels = function(n){format(n / 1000000, scientific = FALSE)}) +
  expand_limits(y = 0.0) +
  geom_errorbar(aes(ymax = meanElapsedTime + seElapsedTime, ymin = meanElapsedTime - seElapsedTime), colour = "black", width = 0.2, alpha = 0.35, position = "dodge") +
  theme_minimal() +
  theme(legend.position = "none", axis.text=element_text(size=24), axis.title=element_text(size=28))

  ggsave("elapsedTime_v_program_1_sm.pdf", width = 7, height = 7)
}
