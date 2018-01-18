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

#dataDir <- file.path(experimentDataDir, "PorcE", "invocation-overhead", "20180104-a003")
#dataDir <- file.path(tmpExperimentDataDir, "20180116_1158")
#d <- readResultsTable(file.path(dataDir, "porce-class-profile-rep5.csv"))

calibrate <- function() {
  dataFromFile <- function(input) {
    d <- readResultsTable(input$file)
    d <- d %>%
      filter(class == "orc.run.porce.Read$Constant") %>%
      mutate(selfper = as.integer(self / hits), file = input$file) %>%
      select(file, everything(), -class, -total)
    d
  }
  dataFiles <- Sys.glob(file.path(tmpExperimentDataDir, "*", "porce-class-profile-rep1[3-9].csv"))
  data <- data.frame(file = dataFiles, stringsAsFactors = F) %>% rowwise() %>% do(dataFromFile(.)) %>% ungroup()

  r <- data %>% bootstrapStatistics(selfper, mean)

  calibrationOffset <<- r$selfper_mean

  r
}

print("Calibration:")
print(calibrate())


dataFiles <- Sys.glob(file.path(tmpExperimentDataDir, "*", "porce-class-profile-rep10.csv"))

options(scipen = 30)

dataFromFile <- function(input) {
  d <- readResultsTable(input$file)
  d <- d %>%
    #mutate(self = self - calibrationOffset * hits, total = total - calibrationOffset * hits) %>%
    mutate(selfper = as.integer(self / hits), totalper = as.integer(total / hits), childrenper = totalper - selfper, file = input$file)
  d
}
dataFiles <- Sys.glob(file.path(tmpExperimentDataDir, "*", "porce-class-profile-rep1[0-9].csv"))
data <- data.frame(file = dataFiles, stringsAsFactors = F) %>% rowwise() %>% do(dataFromFile(.)) %>% ungroup()


r <- data %>% group_by(class) %>% bootstrapStatistics(c("hits", "self", "total", "selfper", "totalper", "childrenper"), mean, R = 1)

#print(kable(r[order(r$self_mean),], format = "rst", format.args = list(big.mark = ",")))

