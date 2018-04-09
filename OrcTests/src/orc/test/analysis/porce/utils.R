
benchmarkProblemNames <- read.csv(file.path(scriptDir, "porce", "benchmark-problem-names.csv"), strip.white = T, header = T) %>%
  mutate(benchmarkNamePrefix = as.character(benchmarkNamePrefix))

addBenchmarkProblemName <- function(.data) {
  .data %>%
    mutate(benchmarkNamePrefix = sapply(strsplit(as.character(benchmarkName), "[- ._]"), function(v) v[1])) %>%
    left_join(benchmarkProblemNames, by = c("benchmarkNamePrefix")) %>%
    select(-benchmarkNamePrefix)
}
