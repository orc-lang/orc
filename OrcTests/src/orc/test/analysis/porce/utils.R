
.benchmarkProblemNames <- read.csv(file.path(scriptDir, "porce", "benchmark-problem-names.csv"), strip.white = T, header = T) %>%
  mutate(benchmarkNamePrefix = as.character(benchmarkNamePrefix))

addBenchmarkProblemName <- function(.data) {
  .data %>%
    mutate(benchmarkNamePrefix = sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])) %>%
    left_join(.benchmarkProblemNames, by = c("benchmarkNamePrefix")) %>%
    select(-benchmarkNamePrefix)
}

loadAndPruneBenchmarkData <- function(dataDir, benchmarkIDCols, runIDCols = c("run"), timeCols = c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), invalidate = T) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = invalidate) %>%
    addBenchmarkProblemName() %>%
    mutate(benchmarkName = factor(paste0(benchmarkName, " (", language, ")")))

  #d <<- data

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(append(benchmarkIDCols, runIDCols), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    group_by_at(vars(one_of(append(c("benchmarkProblemName"), benchmarkIDCols)))) %>%
    filter(!any(rtCompTime < cpuTime * 0.01) | rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    bootstrapStatistics_(timeCols, list(mean = mean), confidence = 0.95, R = 1) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    #group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(nCPUs=1), baseline = elapsedTime_mean_selfbaseline) %>%
    #group_by(benchmarkProblemName) %>% addBaseline(elapsedTime_mean, c(language="Scala", nCPUs=1), baseline = elapsedTime_mean_problembaseline) %>%
    ungroup()
  processedData
}
