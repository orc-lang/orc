library(tidyr)
library(dplyr)
library(knitr)

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs")
tmpExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "runs")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))

process <- function(runName) {
  #dataDir <- file.path(experimentDataDir, "PorcE", "invocation-overhead", "20180104-a003")
  dataDir <- file.path(tmpExperimentDataDir, runName)

  factors <- readResultsTable(file.path(dataDir, "factor-values.csv")) %>%
    select(FactorName, Value) %>% spread(FactorName, Value) %>% mutate_if(is.factor, as.character)
  workerTimes <- readResultsTable(file.path(dataDir, "worker-times.csv"))
  benchmarkTimes <- readResultsTable(file.path(dataDir, "benchmark-times.csv"))

  workerTimes <- workerTimes %>%
    mutate(rep = as.numeric(gsub("rep([0-9]+)$", "\\1", id))) %>%
    select(rep, everything(), -id) %>%
    mutate_at(vars(workerOverhead:workTime), funs(. / 1000 / 1000 / 1000))

  times <- benchmarkTimes %>%
    inner_join(workerTimes, by = "rep") %>%
    filter(rep >= max(rep) - 10)

  results <- times %>%
    summarise_at(c("elapsedTime", "cpuTime", "rtCompTime", "gcTime", "workerOverhead", "schedulingOverhead", "workTime", "nTasks"), funs(mean, sd)) %>%
    select_all(funs(gsub("_mean", "", .)))

  factors %>% t() %>% print()

  results %>% mutate_all(funs(round(., digits = 4))) %>% t() %>% print()

  results %>%
    transmute(cpuTime,
              totalMeasured = workerOverhead + schedulingOverhead + workTime,
              missingTime = max(cpuTime - totalMeasured, 0),
              blockedTime = max(totalMeasured - cpuTime, 0),
              missingProportion = missingTime / ((cpuTime + totalMeasured) / 2),
              blockedProportion = blockedTime / ((cpuTime + totalMeasured) / 2)) %>%
    mutate_all(funs(round(., digits = 4))) %>% t() %>% print()

  results %>%
    transmute(workerTotal = workerOverhead + schedulingOverhead + workTime,
              workerOverheadProportion = workerOverhead / workerTotal,
              schedulingOverheadProportion = schedulingOverhead / workerTotal,
              workProportion = workTime / workerTotal) %>%
    select(-workerTotal) %>%
    mutate_all(funs(round(., digits = 4))) %>% t() %>% print()

  results %>%
    transmute(overheadTotal = workerOverhead + schedulingOverhead,
              nTasks,
              overheadPerTask = overheadTotal / nTasks,
              overheadPerTaskNS = overheadPerTask * 1000 * 1000 * 1000) %>%
    select(-overheadPerTask) %>%
    mutate_all(funs(round(., digits = 4))) %>% t() %>% print()
}

options(scipen = 20)
process("20180121_1324")
process("20180121_1329")
process("20180121_1338")
