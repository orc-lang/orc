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
source(file.path(scriptDir, "porce", "utils.R"))

#dataDir <- file.path(experimentDataDir, "PorcE", "impltype-comparison", "20180415-a001")
#dataDir <- file.path(localExperimentDataDir, "20180730-a010")
dataDir <- file.path(localExperimentDataDir, "20180805-a002")
scalaDataDir <- file.path(localExperimentDataDir, "20180718-a002")

loadData <- function(dataDir) {
  # scalaData <- readMergedResultsTable(scalaDataDir, "benchmark-times", invalidate = F) %>%
  #   filter(language == "Scala")

  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T) %>%
    #bind_rows(scalaData) %>%
    addBenchmarkProblemName()

  #d <<- data

  prunedData <<- data %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    mutate(totalReps = n()) %>%
    ungroup() %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "optLevel", "run"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, optLevel) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime", "totalReps"), mean, confidence = 0.95, R = 1) %>%
    mutate(totalReps = totalReps_mean, totalReps_mean = NULL, totalReps_mean_upperBound = NULL, totalReps_mean_lowerBound = NULL) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    #group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
  processedData
}

loadCompensatedData <- function(dataDir) {
  source(file.path(scriptDir, "porce", "callsTimeAnalysis.R"), local=new.env())
  read.csv(file.path(dataDir, "time", "mean_compensated_elapsed_time.csv"), header = T) %>%
    select(everything(),
           #-elapsedTime_mean, -elapsedTime_mean_lowerBound, -elapsedTime_mean_upperBound,
           -nSamples)
}

#processedData <- loadData(file.path(experimentDataDir, "PorcE", "strong-scaling", "20180203-a009")) %>% mutate(experiment = "old")
#processedData <- processedData %>%
#   rbind(loadData(file.path(localExperimentDataDir, "20180220-a002")) %>% mutate(experiment = "new"))
#processedData <- processedData %>% mutate(experiment = factor(experiment, ordered = T, levels = c("old", "new")))
processedData <- loadData(dataDir) %>% mutate(experiment = factor("only"))
#compensatedData <- loadCompensatedData(dataDir)

print(levels(processedData$benchmarkName))

benchmarkProperties <- {
  r <- read.csv(file.path(scriptDir, "porce", "benchmark-metadata.csv"), strip.white = T, header = T) %>%
    replace_na(list(scalaCompute = T)) %>%
    mutate(granularity = factor(granularity, c("Super Fine", "Fine", "Coarse"), ordered = T)) %>%
    mutate(parallelism = factor(if_else(parallelism == "Par. Col.", "Naïve", as.character(parallelism))))

  levels(r$granularity) <- c("S. Fine", "Fine", "Coarse")

  r
}

processedData <- processedData %>%
  select(-contains("granularity"), -contains("scalaCompute"), -contains("parallelism"), -contains("isBaseline"), -contains("implType"), -contains("optimized")) %>% # Strip out the data we about to add. This allows the script to be rerun without reloading the data.
  left_join(benchmarkProperties, by = c("benchmarkName")) %>%
  group_by(benchmarkProblemName) %>%
  addBaseline(elapsedTime_mean, c(language="Scala"), baseline = elapsedTime_mean_problembaseline) %>%
  addBaseline(cpuTime_mean, c(language="Scala"), baseline = cpuTime_mean_problembaseline) %>%
  ungroup()

# Sample count table

sampleCountData <- processedData %>%
  mutate(implType = factor(if_else(implType == "Orc", paste0("Orc -O", optLevel), as.character(implType)))) %>%
  select(experiment, benchmarkName, nCPUs, implType, nSamples)

sampleCountTable <- function(format) {
  kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

# To quantify dropped elements:

# prunedData %>% select(rep, totalReps)
# prunedData %>% summarise(reps = paste(rep, collapse = ","), totalReps = max(totalReps))

# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

timeOutputDir <- file.path(dataDir, "time")
if (!dir.exists(timeOutputDir)) {
  dir.create(timeOutputDir)
}

print(levels(processedData$benchmarkProblemName))

# for (problem in levels(processedData$benchmarkProblemName)) {
#   ggsave(file.path(outputDir, paste0(problem, "-scaling.pdf")), scalingPlot(problem), width = 7.5, height = 6)
#   ggsave(file.path(outputDir, paste0(problem, "-normPerformance.pdf")), normalizedPerformancePlot(problem), width = 7.5, height = 8)
# }

capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")

sampleCountTable("rst")

# Benchmark vs. Implementation table

useNCPUs <- max(processedData$nCPUs)
useOptLevel <- max(processedData$optLevel, na.rm = T)

implTypes <- c("Orc -O0", paste0("Orc -O", useOptLevel), "Orc*", "Orc+Scala", "Scala")

# compensatedRows <- right_join(processedData, compensatedData, by = c("benchmarkProblemName", "benchmarkName", "nCPUs", "optLevel")) %>%
#   filter(implType == "Orc", optLevel == useOptLevel) %>%
#   mutate(implType = "Orc*",
#          elapsedTime_mean = compensatedElapsedTime_mean,
#          elapsedTime_mean_upperBound = compensatedElapsedTime_mean_upperBound,
#          elapsedTime_mean_lowerBound = compensatedElapsedTime_mean_lowerBound
#         ) %>%
#   select(colnames(processedData))

#rbind(compensatedRows) %>%

longT <- processedData %>%  filter(nCPUs == useNCPUs) %>%
  mutate(implType = factor(if_else(implType == "Orc", paste0("Orc -O", optLevel), as.character(implType)), levels = implTypes))
# %>% mutate(implType = if_else(optimized, paste0(implType, " Opt"), as.character(implType)))

t <- longT %>%
  # filter(optimized | language == "Scala") %>%
  select(benchmarkProblemName, implType, elapsedTime_mean) %>% spread(implType, elapsedTime_mean)

write.csv(t, file = file.path(timeOutputDir, "mean_elapsed_time.csv"), row.names = F)

print(kable(t, "latex"))

# print(t)

filledInData <- processedData %>%
  full_join(processedData %>%
              filter(optLevel == 3, implType == "Orc") %>%
              transmute(benchmarkProblemName, benchmarkName, nCPUs, optLevel = 0, elapsedTime_mean = 10 * 60),
            by = c("benchmarkProblemName", "benchmarkName", "optLevel", "nCPUs")) %>%
  mutate(elapsedTime_mean = if_else(is.na(elapsedTime_mean.x), elapsedTime_mean.y, elapsedTime_mean.x), elapsedTime_mean.x = NULL, elapsedTime_mean.y = NULL)

times2 <-
        processedData %>%
          filter(implType == "Orc", optLevel == 3) %>%
          transmute(benchmarkProblemName, nCPUs, microbenchmark, orc3Time = elapsedTime_mean) %>%
  full_join(processedData %>%
              filter(implType == "Orc", optLevel == 0) %>%
              transmute(benchmarkProblemName, nCPUs, orc0Time = elapsedTime_mean),
            by = c("benchmarkProblemName", "nCPUs")) %>%
  full_join(processedData %>%
              filter(implType == "Scala") %>%
              transmute(benchmarkProblemName, nCPUs, scalaTime = elapsedTime_mean),
            by = c("benchmarkProblemName", "nCPUs")) %>%
  full_join(processedData %>%
              filter(implType == "Orc+Scala", optimized == T) %>%
              transmute(benchmarkProblemName, nCPUs, orcScalaTime = elapsedTime_mean),
            by = c("benchmarkProblemName", "nCPUs")) %>%
  dropBounds()
  #filter(implType == "Orc") %>%
  #mutate(normalizedTime = elapsedTime_mean / orc3Time)

# print(geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / orc3Time) %>% filter(optLevel == 3))$normalizedTime))

times2 <- times2 %>% mutate(orcScalaTime = if_else(is.na(orcScalaTime), orc3Time, orcScalaTime))

normalizedToScalaTimeAtMost <- times2 %>% mutate_at(vars(ends_with("Time")), funs(. / if_else(is.na(scalaTime), 10 * 60, scalaTime)))

print_expr <- function(expr) {
  s <- deparse(substitute(expr))
  cat(paste(s, "=", expr, "\n"))
}

print_expr(
  geomean((normalizedToScalaTimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$orc3Time)
)
print_expr(
  geomean((normalizedToScalaTimeAtMost %>% filter(microbenchmark == T, nCPUs == 24))$orc3Time)
)
print_expr(
  geomean((normalizedToScalaTimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$orcScalaTime)
)

normalizedToOrcScalaTimeAtMost <- times2 %>% mutate_at(vars(ends_with("Time")), funs(. / if_else(is.na(orcScalaTime), 10 * 60, orcScalaTime)))

print_expr(
  geomean((normalizedToOrcScalaTimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$orc3Time)
)
print_expr(
  geomean((normalizedToOrcScalaTimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$orc0Time)
)
print_expr(
  geomean((normalizedToOrcScalaTimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$scalaTime)
)

normalizedToOrc3TimeAtMost <- times2 %>% mutate_at(vars(ends_with("Time")), funs(. / if_else(is.na(orc3Time), 10 * 60, orc3Time)))

print_expr(
  geomean((normalizedToOrc3TimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$orc0Time)
)
print_expr(
  geomean((normalizedToOrc3TimeAtMost %>% filter(microbenchmark == F, nCPUs == 24))$scalaTime)
)


# print_expr(
#   geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / scalaTime) %>% filter(implType == "Orc", optLevel == 3, normalizedTime > 1))$normalizedTime)
# )
# print_expr(
#   geomean((times2 %>% mutate(normalizedTime = elapsedTime_mean / scalaTime) %>% filter(implType == "Orc+Scala", optLevel == 3, normalizedTime > 1))$normalizedTime)
# )

# times2 %>% dropBounds() %>% ggplot(aes(cpuUtilization, normalizedTime, color = benchmarkProblemName)) + geom_point() + xlim(0, 24) + ylim(0, 250) + facet_wrap(~implType)

# t <- longT %>%
#   filter(!optimized | language == "Scala") %>%
#   select(benchmarkProblemName, implType, elapsedTime_mean) %>% spread(implType, elapsedTime_mean)
#
# print(t)

#processedData <- processedData %>% filter(is.element(benchmarkName, includedBenchmarks))
#processedData <- processedData %>% filter(optimized == F)

