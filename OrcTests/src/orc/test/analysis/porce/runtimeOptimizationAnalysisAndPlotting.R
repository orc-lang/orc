# Utilities for processing and plotting PorcE performance data.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

#library(knitr)
#library(tikzDevice)
library(tidyr)

#theme_set(theme_gray())

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))
experimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "experiment-data")
localExperimentDataDir <- file.path(dirname(dirname(dirname(dirname(dirname(scriptDir))))), "OrcTests/runs/")

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))
source(file.path(scriptDir, "plotting.R"))
source(file.path(scriptDir, "porce", "utils.R"))

#dataDir <- file.path(experimentDataDir, "PorcE", "development", "20171111-a003")
dataDir <- file.path(localExperimentDataDir, "20180609-a004")

if(!exists("processedData")) {
  data <- readMergedResultsTable(dataDir, "benchmark-times", invalidate = T)%>%
    addBenchmarkProblemName()

  prunedData <- data %>%
    dropWarmupRepetitionsTimedRuns(c("benchmarkName", "nCPUs", "step"), rep, elapsedTime, 5, 20, 120, minRemaining = 1, maxRemaining = 20) %>%
    # Drop any reps which have more than 1% compilation time.
    filter(rtCompTime < cpuTime * 0.01)

  processedData <- prunedData %>%
    group_by(benchmarkProblemName, language, benchmarkName, nCPUs, step) %>%
    bootstrapStatistics(c("elapsedTime", "cpuTime", "gcTime", "rtCompTime"), mean, confidence = 0.95) %>%
    mutate(cpuUtilization = cpuTime_mean / elapsedTime_mean,
           cpuUtilization_lowerBound = cpuTime_mean_lowerBound / elapsedTime_mean_upperBound,
           cpuUtilization_upperBound = cpuTime_mean_upperBound / elapsedTime_mean_lowerBound) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(step=0), baseline = elapsedTime_mean_none) %>%
    group_by(benchmarkName) %>% addBaseline(elapsedTime_mean, c(step=1), baseline = elapsedTime_mean_pic) %>%
    ungroup()


  processedData <- full_join(processedData, with(processedData, expand.grid(benchmarkName = unique(benchmarkName), nCPUs = unique(nCPUs), step = unique(step))))

  processedData$step <- factor(processedData$step, levels = c(0, 1, 2, 3), labels = c(
    "None",
    "PIC",
    "Inlining",
    "Peephole"
  ))

  processedData <-
    left_join(processedData,
              processedData %>% group_by(benchmarkName) %>%
                summarise(elapsedTime_mean_max = max(elapsedTime_mean, na.rm = T))) %>%
    ungroup() %>%
    addBenchmarkMetadata()
}

paletteValues <- rep(c(rbind(brewer.pal(8, "Dark2"), rev(brewer.pal(12, "Set3")))), 10)
fillPalette <- scale_fill_manual(values = paletteValues)
colorPalette <- scale_color_manual(values = paletteValues)

plotAllData <- function(data) {
  d <- data %>% group_by(benchmarkName, nCPUs, step) %>% mutate(norm = elapsedTime / min(elapsedTime), startTime = cumsum(elapsedTime))
  vis <- list(geom_point(alpha = 0.8, position = position_jitter()), colorPalette)
  p <- d %>% ggplot(aes(x = rep, y = norm, color = benchmarkName)) + vis
  print(p)
  p <- d %>% ggplot(aes(x = startTime, y = norm, color = benchmarkName)) + vis
  print(p)
}

# Turn on to view data and evaluate the number warm up iterations.
#plotAllData(data)

sampleCountData <- processedData %>% transmute(benchmarkName = benchmarkName, config = step, nSamples = nSamples) %>% spread(config, nSamples)

# Sample count table

sampleCountTable <- function(format) {
  #kable(sampleCountData, format = format, caption = "The number of repetitions which were used for analysis from each run.")
}

plotSingleBenchmark <- function(benchmark, labels = TRUE) {
  d <- processedData %>% filter(benchmarkName == benchmark)
  ymax <- max(d[2:length(d),]$elapsedTime_mean / d[2:length(d),]$elapsedTime_mean_none, na.rm = T) * 1.2
  p <- d %>% ggplot(aes(
    x = step,
    fill = step,
    y = elapsedTime_mean / elapsedTime_mean_none)) +
    labs(y = if (labels) "Elapsed Time (normalized)" else NULL, x = NULL) +
    theme_minimal() +
    scale_y_continuous(labels = scales::percent) +
    facet_wrap(~benchmarkName, scales = "free_y") +
    theme(
      text = element_text(size = 7),
      axis.text.y = element_text(size = 6),
      axis.text.x = if (labels)  element_text(angle=90, hjust=1, vjust = 0.4, size = 5, color = "black") else element_blank(),
      panel.grid.major.x = element_line(linetype = 0)) +
    coord_cartesian(ylim = c(0, 1.1 * ymax)) +
    scale_fill_brewer(palette = "Dark2")

  p +
    geom_col(show.legend = F) +
    geom_text(aes(label = if_else(is.na(elapsedTime_mean), "Timeout", ""), y = elapsedTime_mean_max * 0.95),
              position = position_dodge(0.9), vjust = "center", hjust = "right", angle = 90) +
    geom_text(aes(label = if_else(elapsedTime_mean / elapsedTime_mean_none > ymax, "100%", ""), # format(elapsedTime_mean / elapsedTime_mean_none, digits = 2, nsmall=0)
                  y = pmax(pmin(elapsedTime_mean / elapsedTime_mean_none + (0.01 * ymax), ymax * 0.8), 0)
                  ),
              position = position_dodge(0.9), vjust = "middle", hjust = "left", angle = 90, size = 1.8)
}

p <- processedData %>% ggplot(aes(
  x = step,
  fill = step,
  y = elapsedTime_mean, # if_else(is.na(elapsedTime_mean), elapsedTime_mean_max * 1, elapsedTime_mean),
  ymin = elapsedTime_mean_lowerBound,
  ymax = elapsedTime_mean_upperBound)) +
  labs(y = "Elapsed Time (seconds)", x = NULL) +
  theme_minimal() +
  facet_wrap(~benchmarkName, scales = "free_y") +
  theme(axis.text.x = element_text(size=8, angle=40, hjust=1),
        panel.grid.major.x = element_line(linetype = 0)) +
  fillPalette
#scale_fill_brewer(palette = "Dark2")
#coord_cartesian(ylim = c(0, max(processedData$elapsedTime_mean, na.rm = T)))

fullPerformancePlot <- p +
  geom_col(show.legend = F) +
  geom_text(aes(label = if_else(is.na(elapsedTime_mean), "Timeout", ""), y = elapsedTime_mean_max * 0.95),
            position = position_dodge(0.9), vjust = "center", hjust = "right", angle = 90)

print(fullPerformancePlot)

p <- processedData %>% ggplot(aes(
  x = step,
  fill = step,
  y = elapsedTime_mean_none / elapsedTime_mean, # if_else(is.na(elapsedTime_mean), elapsedTime_mean_max * 1, elapsedTime_mean),
  ymin = elapsedTime_mean_none / elapsedTime_mean_lowerBound,
  ymax = elapsedTime_mean_none / elapsedTime_mean_upperBound)) +
  labs(y = "Speed-up", x = NULL) +
  theme_minimal() +
  facet_wrap(~benchmarkName, scales = "free_y") +
  theme(axis.text.x = element_text(size=8, angle=40, hjust=1),
        panel.grid.major.x = element_line(linetype = 0)) +
  fillPalette
#scale_fill_brewer(palette = "Dark2")
#coord_cartesian(ylim = c(0, max(processedData$elapsedTime_mean, na.rm = T)))

fullRelPerformancePlot <- p +
  geom_col(show.legend = F) +
  geom_text(aes(label = if_else(is.na(elapsedTime_mean), "Timeout", ""), y = elapsedTime_mean_max * 0.95),
            position = position_dodge(0.9), vjust = "center", hjust = "right", angle = 90)

print(fullRelPerformancePlot)

  # geom_text(aes(label = format(elapsedTime_mean, digits = 2, nsmall=0),
  #               y = pmax(pmin(elapsedTime_mean + (0.01 * max(elapsedTime_mean)), max(elapsedTime_mean) * 0.9), 0)),
  #           position = position_dodge(0.9), vjust = 0)

# print(fullPerformancePlot)


# Output

outputDir <- file.path(dataDir, "plots")
if (!dir.exists(outputDir)) {
  dir.create(outputDir)
}


#capture.output(sampleCountTable("rst"), file = file.path(outputDir, "usedSampleCounts.rst"), type = "output")
#capture.output(sampleCountTable("latex"), file = file.path(outputDir, "usedSampleCounts.tex"), type = "output")




p <- processedData %>% ggplot(aes(
  x = step,
  fill = step,
  y = cpuUtilization, # if_else(is.na(elapsedTime_mean), elapsedTime_mean_max * 1, elapsedTime_mean),
  ymin = cpuUtilization_lowerBound,
  ymax = cpuUtilization_upperBound)) +
  labs(y = "CPU Utilization", x = NULL) +
  theme_minimal() +
  facet_wrap(~benchmarkName, scales = "free_y") +
  theme(axis.text.x = element_text(size=8, angle=40, hjust=1),
        panel.grid.major.x = element_line(linetype = 0)) +
  fillPalette
#scale_fill_brewer(palette = "Dark2")
#coord_cartesian(ylim = c(0, max(processedData$elapsedTime_mean, na.rm = T)))

fullUtilizationsPlot <- p +
  geom_col_errorbar(show.legend = F)

print(fullUtilizationsPlot)


print(processedData %>% mutate(measuredTime = elapsedTime_mean * nSamples) %>% select(benchmarkName, step, measuredTime, nSamples))


comparisonTable <- processedData %>%
  filter(step == "Peephole", implType == "Orc") %>%
  transmute(benchmarkName,
            unopt_time = c(599.32, 433.62, 192.32, 76.24, 306.61, 65.48, NA, NA, 2.29, NA),
            none_time = elapsedTime_mean_none,
            pic_time = elapsedTime_mean_pic,
            opt_time = elapsedTime_mean) %>%
  replace_na(list(unopt_time = 630, none_time = 630, opt_time = 630)) %>%
  mutate(static_speedup = unopt_time / none_time,
         pic_speedup = none_time / pic_time,
         dynamic_speedup = pic_time / opt_time,
         total_speedup = unopt_time / opt_time) %>%
  mutate(unopt_time = unopt_time / opt_time,
         none_time = none_time / opt_time,
         pic_time = pic_time / opt_time,
         opt_time = 1) %>%
  ungroup()

longTimeComparisonTable <- comparisonTable %>%
  gather(kind, time, ends_with("_time"), factor_key = T) %>%
  select(benchmarkName, kind, time)

longSpeedupComparisonTable <- comparisonTable %>% #select(-total_speedup) %>%
  gather(kind, speedup, ends_with("_speedup"), factor_key = T) %>%
  select(benchmarkName, kind, speedup)

p <- ggplot(longTimeComparisonTable, aes(fill = kind, time, x = benchmarkName)) +
  geom_col(position = position_dodge()) +
  theme(axis.text.x = element_text(size=8, angle=40, hjust=1),
        panel.grid.major.x = element_line(linetype = 0))

print(p)
print(p + scale_y_log10())


ggplot(longSpeedupComparisonTable, aes(fill = kind, speedup, x = benchmarkName)) +
  geom_col(position = position_identity(), alpha = 0.4)



print(geomean(comparisonTable$static_speedup))
print(geomean(comparisonTable$pic_speedup))
print(geomean(comparisonTable$dynamic_speedup))
print(geomean(comparisonTable$total_speedup))


t <- comparisonTable %>%
  mutate(static_saved = unopt_time - none_time, dynamic_saved = none_time - opt_time) %>%
  mutate(static_proportion = static_saved / (static_saved + dynamic_saved), dynamic_proportion = dynamic_saved / (static_saved + dynamic_saved))

print(mean(t$static_speedup))
print(mean(t$total_speedup))
print(mean(t$static_proportion))
print(mean(t$dynamic_proportion))
