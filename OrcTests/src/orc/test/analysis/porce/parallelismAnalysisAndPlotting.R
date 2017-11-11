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

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))

source(file.path(scriptDir, "readMergedResultsTable.R"))

outputFile <- "/home/amp/shared/orc/runs/20171110_1655/schedule_rep3.csv"

if (!exists("vs")) {
  header <- read.csv(outputFile, header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  vs <- fread(outputFile, col.names = names, data.table = F, integer64 = "double") %>%
    mutate_all(function(v) round(to.s(v), usedDigits)) %>%
    filter(realStart > 0)
}

reduce.time.resolution <- function(.data, roundTo, f) {
  .data %>% mutate(time = round(time, roundTo)) %>% group_by(time) %>% summarise_all(f)
}

idealParallelism <-
  with(vs, { tibble(time = c(idealStart, idealEnd), change = c(rep(1, times = length(idealStart)), rep(-1, times = length(idealEnd)))) }) %>%
  group_by(time) %>% tally(change) %>% filter(n != 0) %>% arrange(time) %>% transmute(time = time, parallelism = cumsum(n)) %>% mutate(kind = "Ideal")

realParallelism <-
  with(vs, { tibble(time = round(c(realStart, realEnd), usedDigits), change = c(rep(1, times = length(realStart)), rep(-1, times = length(realEnd)))) }) %>%
  group_by(time) %>% tally(change) %>% filter(n != 0) %>% arrange(time) %>% transmute(time = time, parallelism = cumsum(n)) %>% mutate(kind = "Real")

parallelismFactor <- function(parallelismTable) {
  area <- parallelismTable %>% transmute(area = c(0, diff(time)) * parallelism) %>% sum(na.rm	= T)
  length <- max(parallelismTable$time, na.rm	= T)
  list(average = area / length, area = area, length = length)
}

idealStats <- parallelismFactor(idealParallelism)
realStats <- parallelismFactor(realParallelism)

#tikz(file = "black-scholes.tex", width = 3, height = 2)

bind_rows(reduce.time.resolution(idealParallelism, 2, max), reduce.time.resolution(realParallelism, 2, max)) %>%
  ggplot(aes(time, parallelism)) +
  geom_area(aes(fill = kind), alpha = 0.5, position = position_identity()) +
  geom_hline(data = tibble(kind = c("Ideal", "Real"), average = c(idealStats$average, realStats$average)), aes(yintercept = average, color = kind)) +
  labs(y = "Number of Parallel Tasks", x = "Time (seconds)", color = "", fill = "") +
  coord_cartesian() +
  scale_color_manual(values = c("darkgreen", "darkblue")) +
  scale_fill_manual(values = c("darkgreen", "darkblue"))

#dev.off()
