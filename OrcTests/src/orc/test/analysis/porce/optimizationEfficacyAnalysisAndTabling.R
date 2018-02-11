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

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))


#dataDir <- file.path(experimentDataDir, "PorcE", "strong-scaling", "20171024-a003")
dataDir <- file.path(localExperimentDataDir, "20180203-a009")
#
# Load main data

dataOrct <- readMergedResultsTable(dataDir, "orctimizer-statistics", invalidate = T) %>% filter(run == 0)
dataPorc <- readMergedResultsTable(dataDir, "porc-optimizer-statistics", invalidate = T) %>% filter(run == 0)

dropMiddle <- function(d) {
  d %>% arrange(pass) %>% group_by(benchmarkName) %>% slice(c(1, n())) %>% mutate(optimized = pass != 1, pass = NULL) %>% ungroup()
}

dataOrct <- dataOrct %>% dropMiddle()
dataPorc <- dataPorc %>% dropMiddle()

# Load truffle specialization information

countMatchingLines <- function(str, lines, fixed = T) {
  sum(grepl(str, lines, fixed = fixed))
}

loadTruffleData <- function(benchmarkName, orcFile) {
  n <- sub("^.*/([^/.]*).orc", "\\1", orcFile)
  fp <- file.path(dataDir, "raw-output", paste0(n, "_orc*_24*_truffle-node-specializations_*.txt"))
  #print(fp)
  #print(Sys.glob(fp))
  fn <- first(Sys.glob(fp))
  if (length(fn) > 0) {
    txt <- readLines(fn)

    unoptSpawns <- countMatchingLines("spawn: (1) {}", txt)
    inlinedSpawns <- countMatchingLines("inline: (1) {Catch", txt)
    deinlinedSpawns <- countMatchingLines("spawnAfterInline: (1) {}", txt)

    forcesSingle <- countMatchingLines("| SingleFutureNodeGen", txt)
    nonforcesSingle <- countMatchingLines("SingleFutureNodeGen<nonFuture=Binary(wasTrue=true, wasFalse=false)", txt)
    blockedForcesSingle <- countMatchingLines("SingleFutureNodeGen<.*,unboundFuture=ResettableBranchProfile\\(visited=true\\).*>", txt, fixed = F)

    forcesMulti <- countMatchingLines("| FinishNodeGen", txt)
    blockedForcesMulti <- countMatchingLines("blocked: (1) {}", txt)

    data.frame(benchmarkName,
               unoptSpawns = unoptSpawns + deinlinedSpawns,
               inlinedSpawns = inlinedSpawns - deinlinedSpawns,
               remainingForces = blockedForcesSingle + blockedForcesMulti,
               eliminatedForces = forcesSingle - blockedForcesSingle + forcesMulti - blockedForcesMulti
               )
  } else {
    data.frame(benchmarkName,
               unoptSpawns = NA,
               inlinedSpawns = NA,
               remainingForces = NA,
               eliminatedForces = NA
    )
  }
}


benchmarkProperties <- {
  csv <- "
benchmarkName,                                           granularity, scalaCompute, parallelism, isBaseline
Black-Scholes-naive ,                               Fine,        F,            Naïve,       F
Black-Scholes-partially-seq ,                       Fine,        F,            Naïve,       F
Black-Scholes-par (Scala),                               Fine,        NA,           Par. Col.,   TRUE
Black-Scholes-partitioned-seq ,                     Coarse,      F,            Partition,   F
Black-Scholes-scala-compute ,                       Fine,        T,            Naïve,       F
Black-Scholes-scala-compute-partially-seq ,         Fine,        T,            Naïve,       F
Black-Scholes-scala-compute-partitioned-seq ,       Coarse,      T,            Partition,   F
Dedup-boundedchannel ,                              Coarse,      T,            Thread,      F
Dedup-boundedqueue (Scala),                              Coarse,      NA,           Thread,      F
Dedup-naive ,                                       Fine,        T,            Naïve,       F
Dedup-nestedpar (Scala),                                 Fine,        NA,           Par. Col.,   TRUE
KMeans ,                                            Fine,        F,            Naïve,       F
KMeans-par (Scala),                                      Fine,        NA,           Par. Col.,   TRUE
KMeans-par-manual (Scala),                               Coarse,      NA,           Partition,   F
KMeans-scala-inner ,                                Coarse,      T,            Partition,   F
SSSP-batched ,                                      Fine,        F,            Naïve,       F
SSSP-batched-par (Scala),                                Fine,        NA,           Par. Col.,   TRUE
SSSP-batched-partitioned ,                          Coarse,      F,            Partition,   F
Swaptions-naive-scala-sim ,                         Coarse,      T,            Naïve,       F
Swaptions-naive-scala-subroutines-seq ,             Fine,        T,            Naïve,       F
Swaptions-naive-scala-swaption ,                    Coarse,      T,            Naïve,       F
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

starting <- dataOrct %>% filter(optimized == FALSE)
ending <- dataPorc %>% filter(optimized == TRUE)

dataPorcE <- dataOrct %>% group_by(benchmarkName) %>%
  do(loadTruffleData(first(.$benchmarkName), first(.$orcFile)))

values <- list(
  starting %>% transmute(benchmarkName, startingFutures = Future),
  ending %>% transmute(benchmarkName, endingFutures = NewFuture),
  starting %>% transmute(benchmarkName, startingForces = Force),
  ending %>% transmute(benchmarkName, endingForces = Force),
  starting %>% transmute(benchmarkName, startingSpawns = Future + Parallel + Branch),
  dataPorc %>% filter(optimized == FALSE) %>% transmute(benchmarkName, startingPorcSpawns = Spawn),
  ending %>% transmute(benchmarkName, endingSpawns = Spawn),
  dataPorcE,
  benchmarkProperties
)

join_all <- function(ds, by = NULL) {
  d <- NULL
  for(e in ds) {
    if (is.null(d)) {
      d <- e
    } else {
      d <- left_join(d, e, by = by)
    }
  }
  d
}



r <- join_all(values, by = "benchmarkName") %>% mutate(benchmarkName = factor(benchmarkName))

includedBenchmarks <- {
  txt <- "
  #Black-Scholes-naive
  #Black-Scholes-partially-seq
  Black-Scholes-partitioned-seq
  #Black-Scholes-scala-compute
  Black-Scholes-scala-compute-partially-seq
  Black-Scholes-scala-compute-partitioned-seq
  Dedup-boundedchannel
  Dedup-naive
  KMeans
  KMeans-scala-inner
  SSSP-batched
  SSSP-batched-partitioned
  Swaptions-naive-scala-sim
  Swaptions-naive-scala-subroutines-seq
  #Swaptions-naive-scala-swaption
"
  read.csv(text = txt, strip.white = T, header = F, stringsAsFactors = F)[[1]]
}

r <- r %>% filter(is.element(benchmarkName, includedBenchmarks))

# formatOpt <- function(remaining, starting, percentStatic, percentDynamic) {
#   sprintf("% 4d (% 4d, %2.0f%% + %2.0f%% = %2.0f%%)", remaining, starting, percentStatic * 100, percentDynamic * 100, percentStatic * 100 + percentDynamic * 100)
# }

t <- r %>% transmute(
  benchmarkName,
  if_else(scalaCompute, "Scala", "Orc"),
  parallelism,

  remainingFutures = endingFutures, startingFutures = startingFutures,
  percentStaticFutures = (startingFutures - endingFutures) / startingFutures * 100, percentDynamicFutures = 0,
  percentFutures = percentStaticFutures + percentDynamicFutures,

  remainingForces, startingForces,
  percentStaticForces = (startingForces - endingForces) / startingForces * 100, percentDynamicForces = (endingForces - remainingForces) / startingForces * 100,
  percentForces = percentStaticForces + percentDynamicForces,

  remainingSpawns = unoptSpawns, startingSpawns,
  percentStaticSpawns = (startingSpawns - endingSpawns) / startingSpawns * 100, percentDynamicSpawns = (endingSpawns - unoptSpawns) / startingSpawns * 100,
  percentSpawns = percentStaticSpawns + percentDynamicSpawns
) %>% mutate_if(is.numeric, round)

print("percentFutures")
print(round(geomean(t$percentFutures / 100) * 100))
print("percentForces")
print(round(geomean(t$percentForces / 100) * 100))
print("percentSpawns")
print(round(geomean(t$percentSpawns / 100) * 100))

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

write.csv(t, file = file.path(outputDir, "optimization.csv"), row.names = F)

print(kable(t, "latex"))
