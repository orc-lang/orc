library(data.table)
library(tidyr)
library(dplyr)
library(ggplot2)

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))

source(file.path(scriptDir, "readMergedResultsTable.R"))
source(file.path(scriptDir, "analysis.R"))

dataDir <- "20180101_1622"
dataDir <- "20171231_1947"

outputFileFmt <- paste0("/home/amp/shared/orc/runs/", dataDir, "/runtime_profile_rep%d.csv")
indexFile <- paste0("/home/amp/shared/orc/runs/", dataDir, "/porc-ast-indicies.csv")

header <- read.csv(sprintf(outputFileFmt, 18), header = FALSE, nrows = 1)
names <- vapply(header[1,], cleanColumnName, "")
data <- setNames(data.frame(matrix(ncol = length(names), nrow = 0)), names)
for (i in 18:21) {
  outputFile <- sprintf(outputFileFmt, i)
  d <- fread(outputFile, col.names = names)
  data <- rbind(data, d)
}

if(!exists("index") || !exists("oldDataDir") || dataDir != oldDataDir) {
  header <- read.csv(indexFile, header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  index <- read.csv(indexFile, col.names = names)
}

stats <- data %>%
  group_by(callId) %>% bootstrapStatistics(c("cdTime", "jdTime", "siteTime"), mean, R=1, trim=0.05) %>%
  left_join(index, by = c("callId" = "i"))

s <- stats %>%
  filter(nSamples > 1000)

print(s)
