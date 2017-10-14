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

library(plyr)
library(ggplot2)

repetitionTimes <- read.csv("repetition-times.csv")
names(repetitionTimes) <- c("program", "repeatRead", "numInputFiles", "dOrcNumRuntimes", "repetitionNumber", "elapsedTime")

elapsedTimeSummary <- ddply(repetitionTimes, c("program", "repeatRead", "numInputFiles", "dOrcNumRuntimes"), summarise, nElapsedTime = length(elapsedTime), meanElapsedTime = mean(elapsedTime), sdElapsedTime = sd(elapsedTime), seElapsedTime = sdElapsedTime / sqrt(nElapsedTime))


for (currProgram in unique(elapsedTimeSummary$program)) {
  if (currProgram != "WordCount.java") {
    ggplot(subset(elapsedTimeSummary, program == currProgram), aes(x = factor(numInputFiles), y = meanElapsedTime, fill = factor(dOrcNumRuntimes), position = "dodge")) +
        geom_col(position = "dodge") +
        ggtitle(currProgram) +
        xlab("Number of files read") +
        labs(fill = "Cluster size [Number of d-Orc runtimes]") +
        scale_y_continuous(name = "Elapsed time (s)", labels = function(n){format(n / 1000000, scientific = FALSE)}) +
    #    geom_errorbar(aes(ymax = meanElapsedTime + seElapsedTime, ymin = meanElapsedTime - seElapsedTime), position = "dodge") +
        theme_minimal() +
        theme(legend.justification = c(0, 1), legend.position = c(0, 1))
  } else {
    ggplot(subset(elapsedTimeSummary, program == currProgram), aes(x = factor(numInputFiles), y = meanElapsedTime, position = "dodge")) +
        geom_col(position = "dodge") +
        ggtitle(currProgram) +
        xlab("Number of files read") +
        scale_y_continuous(name = "Elapsed time (s)", labels = function(n){format(n / 1000000, scientific = FALSE)}) +
    #    geom_errorbar(aes(ymax = meanElapsedTime + seElapsedTime, ymin = meanElapsedTime - seElapsedTime), position = "dodge") +
        theme_minimal() +
        theme(legend.justification = c(0, 1), legend.position = c(0, 1))
  }

    ggsave(paste0("elapsedTime_", currProgram, ".pdf"))
}
