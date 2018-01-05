library(data.table)
library(tidyr)
library(dplyr)
library(ggplot2)
library(simpleRCache)

setCacheRootPath()

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
readMergedResultsTableCached <- addMemoization(readMergedResultsTable)
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))

#dataDir <- file.path(experimentDataDir, "PorcE", "invokationOverhead", "20180102-a004")
dataDir <- file.path(localExperimentDataDir, "20180104-a003")

overallTime <- readMergedResultsTableCached(dataDir, "benchmark-times", invalidate=T)

orcFileToPrefix <- function(fn) {
  sub(".*?([^/.]*)\\.orc", "\\1", fn)
}

readIndex <- function(program) {
  indexFile <- Sys.glob(file.path(dataDir, paste0("raw-output/", orcFileToPrefix(program), "_*_porc-ast-indicies_0.csv")))
  header <- read.csv(indexFile, header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  print(noquote(paste("Loading Porc index from", indexFile)))
  read.csv(indexFile, col.names = names)
}

readIndexCached <- addMemoization(readIndex)

readRuntimeProfile <- function(program, rep) {
  outputFile <- Sys.glob(file.path(dataDir, paste0("raw-output/", orcFileToPrefix(program), "_*_runtime_profile_rep", rep, "_0.csv.gz")))

  index <- readIndexCached(program)

  header <- read.csv(gzfile(outputFile), header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  print(noquote(paste("Loading runtime profile from", outputFile)))
  data <- fread(paste0("zcat ", outputFile), col.names = names)

  list(data = data, index = index, program = factor(program), rep = as.integer(rep))
}

readRuntimeProfileCached <- addMemoization(readRuntimeProfile)

programReps <- overallTime %>% group_by(orcFile) %>%
  summarise(lastRep = max(rep), repTime = mean(elapsedTime)) %>% transmute(program = orcFileToPrefix(orcFile), lastRep = as.integer(lastRep), repTime = repTime) %>%
  filter(lastRep > 0) %>% mutate(lastRep = lastRep - 1) %>% filter(lastRep >= 0)

computeSiteAverages <- function(data) {
  r <- data$data %>%
    group_by(callId) %>% bootstrapStatistics(c("cdTime", "jdTime", "siteTime"), mean, R=1, trim=0.05) %>%
    left_join(data$index, by = c("callId" = "i"))
  r$program <- data$program
  r$rep <- data$rep
  select(r, program, rep, everything())
}

computeTotalTimes <- function(data) {
  r <- data$data %>%
    mutate(cdTime = cdTime / 1000 / 1000 / 1000, jdTime = jdTime / 1000 / 1000 / 1000, siteTime = siteTime / 1000 / 1000 / 1000) %>%
    ungroup() %>% bootstrapStatistics(c("cdTime", "jdTime", "siteTime"), sum, R=1, trim=0.05) %>% dropBounds()
  r$program <- data$program
  r$rep <- data$rep
  select(r, program, rep, everything())
}

computeOverheads <- function(data) {
  overall <- overallTime %>% filter(grepl(paste0(".*", data$program, ".orc"), orcFile), rep == data$rep)
  cpuTime <- overall$cpuTime
  times <- computeTotalTimes(data)
  times$cpuTime <- cpuTime
  times$elapsedTime <- overall$elapsedTime
  mutate_at(times, ends_with("_sum", vars = colnames(times)), function(v) v / cpuTime)
}

overheads <- addMemoization(function(x) {
  programReps %>% rowwise() %>%
    do(computeOverheads(readRuntimeProfileCached(as.character(.$program), .$lastRep))) %>%
    ungroup() %>%
    mutate_if(is.character, factor)
})(programReps)

siteAverages <- addMemoization(function(x) {
  programReps %>% rowwise() %>%
    do(computeSiteAverages(readRuntimeProfileCached(as.character(.$program), .$lastRep))) %>%
    ungroup() %>%
    mutate_if(is.character, factor)
})(programReps)

myTheme <- list(
  theme_minimal(),
  scale_color_brewer(palette="Dark2"),
  scale_fill_brewer(palette="Dark2"),
  theme(axis.text.x = element_text(angle=-15, hjust=0.2))
)

siteAveragesPlotData <- siteAverages %>%
  group_by(program) %>% mutate(proportion = nSamples / sum(nSamples), overhead_us = (cdTime_mean + jdTime_mean) / 1000) %>% ungroup()
siteAveragesPlotMax <- max(filter(siteAveragesPlotData, proportion > 0.01, overhead_us < 100)$overhead_us) * 2

siteAveragesPlotPointsOnly <- siteAveragesPlotData %>%
  ggplot(aes(x = program, y = overhead_us, color = jdTime_mean > 0, size = proportion * 100, alpha = proportion)) +
  geom_point(position = position_jitter(0.2)) +
  coord_cartesian(ylim = c(0, siteAveragesPlotMax)) +
  scale_alpha_continuous(range = c(0.25, 0.8)) +
  scale_size_area() +
  labs(y = "Call dispatch time (us)", color = "Is Java?", size = "% invokations") +
  guides(alpha = FALSE) +
  myTheme

siteAveragesPlot <- siteAveragesPlotData %>%
  ggplot(aes(x = program, y = overhead_us, weight = proportion)) +
  geom_violin(adjust = 0.5) +
  geom_point(aes(color = jdTime_mean > 0, size = proportion * 100, alpha = proportion), position = position_jitter(0.1)) +
  scale_size_area() +
  scale_alpha_continuous(range = c(0.25, 0.8)) +
  coord_cartesian(ylim = c(0, siteAveragesPlotMax)) +
  labs(y = "Call dispatch time (us)", color = "Is Java?", size = "% invokations") +
  guides(alpha = FALSE) +
  myTheme

overheadsPlot <- overheads %>% mutate(elapsedProportion = elapsedTime / cpuTime) %>%
  gather(key = region, value = time_sum, cdTime_sum, jdTime_sum, siteTime_sum, elapsedProportion) %>%
  mutate(region = factor(region, levels = c("cdTime_sum", "jdTime_sum", "siteTime_sum", "elapsedProportion"), labels = c("Dispatch", "Java Disp.", "Invoked Site", "Total Elapsed"))) %>%
  ggplot(aes(x = program, y = time_sum, fill = region)) +
  geom_col(position = position_dodge()) +
  labs(y = "Proportion of time", fill = "") +
  coord_cartesian(ylim = c(0, 1)) +
  myTheme

print(siteAveragesPlot)
print(overheadsPlot)

# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}

ggsave(file.path(outputDir, "siteAveragesPlot"), siteAveragesPlot, "png", width = 7.5, height = 6)
ggsave(file.path(outputDir, "overheadsPlot"), overheadsPlot, "png", width = 7.5, height = 6)


