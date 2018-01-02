# Utilities for analysing performance data
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(dplyr)
library(ggplot2)
library(boot)

trimVector <- function(data, fraction) {
  nDrop <- floor(length(data) * fraction)
  data[order(data)[nDrop : (length(data)-nDrop)]]
}

# Estimate (very poorly) the number of warm-up repetitions in data.
#
# This finds the first index, n, in data such that:
#   rem <- data[n:length(data)]
#   sd(rem) / mean(rem) < threshold
#
# This will never leave less than minRemaining remaining elements.
#
# This function is useful for quick output, but should not be trusted.
# As such it outputs a warning when it is called.
#
estimateWarmupRepetitions <- function(data, threshold = 1, minRemaining = 5) {
  warning("Estimating the number of warm-up repetitions using a really dumb technique. The the results should be treated as preliminary at best.", immediate. = TRUE)
  len <- length(data)
  n <- min(len, which(sapply(0:len, function(n) {
    tail <- data[n:len]
    sd(tail) / mean(tail)
  }) < threshold))
  max(min(len - minRemaining, n), 0)
}

# TODO: Documentation
dropWarmupRepetitionsTimedRuns <- function(.data, runIDCol, repetitionCol, timeCol, minWarmupReps, maxWarmupReps, maxWarmupTime, minRemaining = 3, maxRemaining = NA) {
  runIDName <- if (is.name(substitute(runIDCol))) {
    deparse(substitute(runIDCol))
  } else runIDCol
  repetitionColName <- if (is.name(substitute(repetitionCol))) {
    deparse(substitute(repetitionCol))
  } else repetitionCol
  timeColName <- if (is.name(substitute(timeCol))) {
    deparse(substitute(timeCol))
  } else timeCol

  f <- function(group) {
    repsMinPred <- group[[repetitionColName]] >= minWarmupReps
    minRemPred <- group[[repetitionColName]] > (max(group[[repetitionColName]]) - minRemaining)
    repsPred <- group[[repetitionColName]] >= maxWarmupReps
    timePred <- lag(cumsum(group[[timeColName]]), default = 0) >= maxWarmupTime
    pred <- ((repsPred | timePred) & repsMinPred) | minRemPred
    used <- group[pred, ]
    if (is.na(maxRemaining) || is.null(maxRemaining)) {
      used
    } else {
      used[1:min(length(used[[repetitionColName]]), maxRemaining),]
    }
  }
  .data %>% group_by_at(runIDName) %>% do(f(.)) %>% ungroup()
}


# Drop the first warmupReps repetitions from every run in data.
#
# This guesses the name of the repetition number column. If it fails to
# guess the value you will need to provide it with the repetitionColName
# parameter.
#
dropWarmupRepetitions <- function(data, warmupReps, repetitionColName = NA) {
  if (is.na(repetitionColName)) {
    existingNames <- colnames(data)
    guessNames <- c("rep", "repetition", "RepetitionNumber", "repetitionNumber", "Repetition.number", "Repetition", "Rep")
    i <- min(Inf, which(guessNames %in% existingNames))
    if (is.infinite(i)) {
      stop("Could not find the repetition number column. Please specify its name with repetitionColName. The columns I can see are: ", deparse(existingNames))
    }
    repetitionColName <- guessNames[i]
  }
  data[data[, repetitionColName] > warmupReps, ]
}

# Compute a statistic and bounds on it's value using the bootstrap method.
#
# data should be a vector. statistic should be a function.
# confidence and R are specify the confidence of the bounds and the number of
# bootstrap replicas respectively.
bootstrapStatistic <- function(data, statistic, confidence = 0.95, R = 10000, trim = 0, statName = NA) {
  if (is.na(statName))
    statName <- deparse(substitute(statistic))

  if (length(data) > 2 && R > 1) {
    b <- boot(data, function(d, sel) {
      if(trim > 0)
        statistic(d[sel], trim = trim)
      else
        statistic(d[sel])
    }, R)
    ci <- boot.ci(b, conf=confidence, type="perc")
    #stopifnot(ci$t0 == statistic(data))
    s <- ci$t0
    lowerBound <- ci$percent[4]
    upperBound <- ci$percent[5]
    confidence <- ci$percent[1]
    r <- data.frame(statistic = statName, value = s, upperBound = upperBound, lowerBound = lowerBound, confidence = confidence, nSamples = length(data))
    r
  } else {
    s <- if(trim > 0)
      statistic(data, trim = trim)
    else
      statistic(data)
    if(identical(statistic, mean)) {
      sd <- if(trim > 0)
        sd(trimVector(data, trim))
      else
        sd(data)
      lowerBound <- s - sd
      upperBound <- s + sd
    } else {
      lowerBound <- NA
      upperBound <- NA
    }
    confidence <- 0
    r <- data.frame(statistic = statName, value = s, upperBound = upperBound, lowerBound = lowerBound, confidence = confidence, nSamples = length(data))
    r
  }
}

.bootstrapStatistics_Internal <- function(data, colNames, statistics, confidence, R, trim) {
  r <- data.frame()
  for (colName in colNames) {
    for (statName in names(statistics)) {
      stat = statistics[[statName]]
      results <- bootstrapStatistic(data[[colName]], stat, confidence, R, trim, statName = statName)
      statColName <- function(n) paste(colName, statName, n, sep = "_")
      r[1, paste(colName, statName, sep = "_")] <- results["value"]
      r[statColName("upperBound")] <- results["upperBound"]
      r[statColName("lowerBound")] <- results["lowerBound"]
    }
  }
  r["confidence"] <- results["confidence"]
  r["nSamples"] <- results["nSamples"]
  r
}

# Compute statistics on specific columns of the data using bootstrapStatistic.
#
# col can either be a column name (either as an identifier or as a string) or a set
# of column names (as a vector of strings). statistic
# can be either a single function or any number of named functions specified as
# c(funcName = func, ...).
#
# Data must be a grouped tibble. This is designed to be used with dplyr as follows:
#   rawData %>% group_by(...) %>% bootstrapStatistics(...)
# For example,
#   rawData %>% group_by(benchmarkName, nCPUs) %>% bootstrapStatistics(vars(elapsedTime, cpuTime), c(mean = mean, median = median), confidence = 0.9)
# or,
#   rawData %>% group_by(benchmarkName, nCPUs) %>% bootstrapStatistics(elapsedTime, mean)
# rawData can be data from readMergedResultsTable or almost any source.
#
bootstrapStatistics <- function(.data, col, statistic, confidence = 0.95, R = 10000, trim = 0) {
  # TODO: Add support for vars arguments.
  colName <- if (is.name(substitute(col))) {
    deparse(substitute(col))
  } else
    col
  statistics <- if (is.name(substitute(statistic))) {
    l <- list()
    l[[deparse(substitute(statistic))]] <- statistic
    l
  } else statistic
  do(.data, .bootstrapStatistics_Internal(., colName, statistics, confidence, R, trim))
}

# Add a baseline column for sourceCol (or each if it is a vector of column names).
#
# requirements should be a vector of named values, for example c(nCPUs = 1),
# the baseline is taked from the row which has matching values. The baseline
# column is named the same as the source column except with "_baseline"
# appended.
#
# sourceCol can either be a vector of strings or a single string or an unquoted name.
#
# Example:
#   d %>% group_by(benchmarkName) %>% addBaseline(c("elapsedTime_mean", "cpuTime_mean"), c(nCPUs = 1))
# This will add baseline columns for elapsedTime_mean and cpuTime_mean where the baseline is taken
# from the rows where nCPUs = 1.
#
addBaseline <- function(.data, sourceCol, requirements, baseline = NA) {
  # TODO: Add support for vars arguments.
  sourceColNames <- if (is.name(substitute(sourceCol))) {
    deparse(substitute(sourceCol))
  } else {
    sourceCol
  }
  baselineName <- if (is.name(substitute(baseline))) {
    deparse(substitute(baseline))
  } else {
    baseline
  }

  if (length(sourceColNames) > 1 && !is.na(baselineName)) {
    stop("The baseline parameter can only be used if there is only one source column.")
  }

  matchesRequirements <- function(d) {
    result <- NULL
    for(matchColName in names(requirements)) {
      matchValue = requirements[[matchColName]]
      if (matchColName %in% colnames(d)) {
        r <- d[[matchColName]] == matchValue
      } else {
        stop(matchColName, " is not a column in the data. Check your requirements argument for mispelled names.")
      }
      if(is.null(result))
        result <- r
      else
        result <- (result & r)
    }
    result
  }

  f <- function(group) {
    whichMatch <- which(matchesRequirements(group))
    nMatching = length(whichMatch)
    if (nMatching >= 1) {
      baselineRow <- group[min(whichMatch), ]

      if (nMatching > 1)
        warning("There should be exactly 1 matching value: found ", nMatching, ". (requirments = ", deparse(requirements), ")\n",
                "Arbitrarily choosing:\n", paste(capture.output(baselineRow), collapse="\n"), "\n",
                "From the available choices:\n", paste(capture.output(group[whichMatch, ]), collapse="\n"))

      for (sourceColName in sourceColNames) {
        if (is.na(baselineName)) {
          baselineColName <- paste(sourceColName, "baseline", sep="_")
        } else {
          baselineColName <- baselineName
        }
        group[[baselineColName]] <- rep(baselineRow[[sourceColName]], length(group[[sourceColName]]))
      }
    } else {
      warning("There should be exactly 1 matching value: found ", nMatching, " (requirments = ", deparse(requirements), ")")
      for (sourceColName in sourceColNames) {
        if (is.na(baselineName)) {
          baselineColName <- paste(sourceColName, "baseline", sep="_")
        } else {
          baselineColName <- baselineName
        }
        group[[baselineColName]] <- rep(NA, length(group[[sourceColName]]))
      }
    }
    group
  }
  do(.data, f(.))
}
