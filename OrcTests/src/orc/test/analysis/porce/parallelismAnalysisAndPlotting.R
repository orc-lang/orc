# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(igraph)
library(tidyr)
library(dplyr)
library(ggplot2)
library(data.table)

to.s <- function(ns) {
  ns / 1000 / 1000 / 1000
}

replace_na.numeric <- function(v, d) {
  if_else(is.na(v), d, v)
}

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))

source(file.path(scriptDir, "readMergedResultsTable.R"))

outputFile <- "/home/amp/shared/orc/runs/20171108_2013/trace_rep9.csv"

if (!exists("vs")) {
  if (!exists("rawdata")) {
    header <- read.csv(outputFile, header = FALSE, nrows = 1)
    names <- vapply(header[1,], cleanColumnName, "")
    rawdata <- fread(outputFile, col.names = names, data.table = F, integer64 = "double")

    rawdata <- rawdata %>% transmute(time = time, type = type, value1 = from, value2 = to)
  }

  edges <- rawdata %>% filter(type == "TaskPrnt") %>% transmute(value1 = as.character(value1), value2 = as.character(value2))
  vertices <- rawdata %>% filter(type != "TaskPrnt") %>%
    bind_rows(rawdata %>% filter(type == "TaskStrt") %>% transmute(type = "TaskETpe", value1 = value1, time = value2)) %>%
    select(-value2) %>% mutate(value1 = as.character(value1)) %>%
    spread(type, time) %>%
    transmute(name = value1, start = to.s(TaskStrt - min(TaskStrt, na.rm = T)), end = to.s(TaskEnd - min(TaskStrt, na.rm = T)),
              len = end - start, executionType = TaskETpe)

  vertices <- full_join(vertices, tibble(name = unique(c(edges$value1, edges$value2))))

  edges <- left_join(edges, vertices %>% transmute(value1 = name, weight = -replace_na(len, 0)))

  g <- graph_from_data_frame(edges)

  initialVerts <- which(degree(g, V(g), mode = "in") == 0)

  root <- "root"

  g <- g %>% add_vertices(1, name = root) %>% add_edges(c(rbind("root", names(initialVerts))), weight = 0)

  longestPath <- distances(g, root, mode = "out")
  V(g)$idealStart <- -c(longestPath)

  # V(g)$level <- -c(distances(g, root, weights = rep(-1, length.out = length(E(g))), mode = "out"))

  vs <- igraph::as_data_frame(g, what = "vertices")

  vs <- left_join(vertices, vs)

  rm(edges, vertices, g, longestPath)
}

idealParallelism <-
  with(vs, { tibble(time = c(idealStart, idealStart + replace_na(len, 0)), change = c(rep(1, times = length(idealStart)), rep(-1, times = length(idealStart)))) }) %>%
  group_by(time) %>% tally(change) %>% filter(n != 0) %>% arrange(time) %>% transmute(time = time, parallelism = cumsum(n))

withLen <- which(!is.na(vs$start))
realParallelism <-
  with(vs, { tibble(time = c(start[withLen], end[withLen]), change = c(rep(1, times = length(withLen)), rep(-1, times = length(withLen)))) }) %>%
  group_by(time) %>% tally(change) %>% filter(n != 0) %>% arrange(time) %>% transmute(time = time, parallelism = cumsum(n))

parallelismFactor <- function(parallelismTable) {
  area <- parallelismTable %>% transmute(area = c(0, diff(time)) * parallelism) %>% sum(na.rm	= T)
  length <- max(parallelismTable$time, na.rm	= T)
  area / length
}

ggplot(mapping = aes(time, parallelism)) + geom_line(data = idealParallelism, color = "green") + geom_line(data = realParallelism, color = "blue")

#print(max(vs$level))
#print(max((vs %>% group_by(level) %>% summarise(n = n()))$n))

#ggplot(vs, aes(x = level)) +
#  geom_bar() + coord_cartesian(ylim=c(0, 100))

vs %>%
  ggplot(aes(x = level)) +# coord_cartesian(ylim=c(0, 20)) +
  geom_linerange(aes(
    ymin = to.s(idealStart),
    ymax = to.s(idealStart + len)),
    alpha = 0.8,
    size = 1
  ) +
  geom_point(aes(
    y = to.s(idealStart),
    color = is.na(len)),
    alpha = 0.8,
    size = 1
  )
#+ geom_bar(aes(fill = factor(is.na(len))), position = position_stack(), width = 1)

# vs %>%
#   ggplot(aes(x = level)) +# coord_cartesian(ylim=c(0, 20)) +
#   geom_linerange(aes(
#     ymin = replace_na(to.ms(start)/1000, -1),
#     ymax = replace_na(to.ms(end)/1000, 0),
#     #size = replace_na(to.ms(len), 1000),
#     color = is.na(len)),
#     alpha = 0.8,
#     size = 1
#   ) +
#   geom_point(aes(
#     y = if_else(is.na(start), -1, to.ms(start)/100),
#     color = is.na(len)),
#     alpha = 0.8,
#     size = 1
#   )
# #+ geom_bar(aes(fill = factor(is.na(len))), position = position_stack(), width = 1)
#

r <- vs %>% group_by(level) %>% filter(len > 50000)


r %>% summarise(n = n()) %>%
  ggplot(aes(x = level, y = n)) + geom_line()


ggplot(r, aes(x = level)) +# coord_cartesian(ylim=c(0, 20)) +
  geom_linerange(aes(
    ymin = to.s(idealStart),
    ymax = to.s(idealStart + len),
    color = is.na(len)),
    alpha = 0.8,
    size = 1
  ) +
  geom_point(aes(
    y = to.s(idealStart),
    color = is.na(len),
    size = len),
    alpha = 0.2
  )
  #+ geom_bar(aes(fill = factor(is.na(len))), position = position_stack(), width = 1)

# vs %>%
#   ggplot(aes(x = len)) + coord_cartesian(xlim=c(0, 200000)) + geom_histogram(binwidth = 1000)

# 50000
