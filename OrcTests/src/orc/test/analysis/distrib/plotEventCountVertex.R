# plotEventCountVertex.R -- R script to plot event counts from Vertex DistribScaleTest runs
# Project OrcTests
#
# Created by jthywiss on Mar 18, 2019.
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

eventCounts <- read.csv("eventCount.csv")
names(eventCounts) <- c("program", "numVertices", "probEdge", "dOrcNumRuntimes", "eventType", "count")

options(tibble.print_max = Inf, tibble.width = Inf)

for (currProgram in unique(eventCounts$program)) {
  for (currEventType in unique(eventCounts[eventCounts$program == currProgram,]$eventType)) {

    ggplot(eventCounts[eventCounts$program == currProgram & eventCounts$eventType == currEventType,], aes(x = dOrcNumRuntimes, y = count, group = factor(numVertices), colour = factor(numVertices), shape = factor(numVertices))) +
    geom_line() +
    geom_point(size = 3) +
    ggtitle(paste(currProgram, "Run", runNumber)) +
    xlab("Cluster size [Number of d-Orc runtimes]") +
    labs(colour = "Number of vertices", shape = "Number of vertices") +
    scale_y_continuous(name = paste("Count of ", currEventType, " on leader"), labels = function(n){format(n, scientific = FALSE)}) +
    expand_limits(x = 1, y = 0.0) +
    # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
    theme_minimal() +
    theme(legend.justification = c(0, 1), legend.position = c(0, 1))

    ggsave(paste0("eventType_v_numRuntime_", currProgram, "_", gsub("[ -/:-@[-`{-~]+", "_", currEventType), ".pdf"), width = 7, height = 7)

  }
}

for (currProgram in unique(eventCounts$program)) {
  for (currEventType in unique(eventCounts[eventCounts$program == currProgram,]$eventType)) {

    ggplot(eventCounts[eventCounts$program == currProgram & eventCounts$eventType == currEventType,], aes(x = numVertices, y = count, group = factor(dOrcNumRuntimes), colour = factor(dOrcNumRuntimes), shape = factor(dOrcNumRuntimes))) +
    geom_line() +
    geom_point(size = 3) +
    ggtitle(paste(currProgram, "Run", runNumber)) +
    xlab("Number of vertices") +
    labs(colour = "Cluster size [Number of d-Orc runtimes]", shape = "Cluster size [Number of d-Orc runtimes]") +
    scale_y_continuous(name = paste("Count of ", currEventType, " on leader"), labels = function(n){format(n, scientific = FALSE)}) +
    expand_limits(x = 1, y = 0.0) +
    # geom_errorbar(aes(ymax = speedupSeMax, ymin = speedupSeMin), width = 0.2, alpha = 0.35) +
    theme_minimal() +
    theme(legend.justification = c(0, 1), legend.position = c(0, 1))

    ggsave(paste0("eventType_v_inputSize_", currProgram, "_", gsub("[ -/:-@[-`{-~]+", "_", currEventType), ".pdf"), width = 7, height = 7)

  }
}
