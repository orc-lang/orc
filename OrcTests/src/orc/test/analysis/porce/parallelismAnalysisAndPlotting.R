# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

#library(igraph)
library(tikzDevice)
library(tidyr)
library(dplyr)
library(ggplot2)
library(data.table)

to.s <- function(ns) {
  ns / 1000 / 1000 / 1000
}

usedDigits <- 4

replace_na.numeric <- function(v, d) {
  if_else(is.na(v), d, v)
}

reduce.time.resolution <- function(.data, roundTo, f) {
  .data %>% mutate(time = round(time, roundTo)) %>% group_by(time) %>% summarise_all(f)
}

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))

loadProcessParallelism <- function(outputFile) {
  header <- read.csv(outputFile, header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  vs <- fread(outputFile, col.names = names, data.table = F, integer64 = "double") %>%
    mutate_all(function(v) round(to.s(v), usedDigits)) %>%
    filter(realStart > 0)

  idealParallelism <-
    with(vs, { tibble(time = c(idealStart, idealEnd), change = c(rep(1, times = length(idealStart)), rep(-1, times = length(idealEnd)))) }) %>%
    group_by(time) %>% tally(change) %>% filter(n != 0) %>% arrange(time) %>% transmute(time = time, parallelism = cumsum(n)) %>% mutate(kind = "Ideal")

  realParallelism <-
    with(vs, { tibble(time = c(realStart, realEnd), change = c(rep(1, times = length(realStart)), rep(-1, times = length(realEnd)))) }) %>%
    group_by(time) %>% tally(change) %>% filter(n != 0) %>% arrange(time) %>% transmute(time = time, parallelism = cumsum(n)) %>% mutate(kind = "Real")

  parallelismFactor <- function(parallelismTable) {
    area <- parallelismTable %>% transmute(area = c(0, diff(time)) * parallelism) %>% sum(na.rm	= T)
    length <- max(parallelismTable$time, na.rm	= T)
    list(average = area / length, area = area, length = length)
  }

  idealStats <- parallelismFactor(idealParallelism)
  realStats <- parallelismFactor(realParallelism)

  #print("Real:")
  #print(str(realStats))
  #print("Ideal:")
  #print(str(idealStats))

  g <- bind_rows(reduce.time.resolution(idealParallelism, 2, max), reduce.time.resolution(realParallelism, 2, max)) %>%
    ggplot(aes(time, parallelism)) +
    geom_area(aes(fill = kind), alpha = 0.5, position = position_identity()) +
    geom_hline(data = tibble(kind = c("Ideal", "Real"), average = c(idealStats$average, realStats$average)), aes(yintercept = average, color = kind)) +
    labs(y = "Number of Parallel Tasks", x = "Time (seconds)", color = "", fill = "") +
    coord_cartesian() +
    scale_color_manual(values = c("darkgreen", "darkblue")) +
    scale_fill_manual(values = c("darkgreen", "darkblue"))

  list(plot = g, real = realStats, ideal = idealStats)
}

dataDir <- file.path(experimentDataDir, "PorcE", "parallelism", "20171111_a100")

# Includes: Black-scholes, Swaptions, SSSP
# Does not yet have: k-Means
inputs <- Sys.glob(file.path(dataDir, "*", "schedule_rep1.csv"))

for (i in inputs) {
  print(i)
  print(loadProcessParallelism(i))
}
