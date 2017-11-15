library(igraph)
library(tidyr)
library(dplyr)
library(ggplot2)

scriptDir <- normalizePath(".") # dirname(dirname(sys.frame(1)$ofile))

source(file.path(scriptDir, "readMergedResultsTable.R"))

outputFile <- "/home/amp/shared/orc/runs/20171106_2045/trace.csv"

header <- read.csv(outputFile, header = FALSE, nrows = 1)
names <- vapply(header[1,], cleanColumnName, "")
data <- read.csv(outputFile, col.names = names)

data <- data %>% transmute(time = Time.1, type = EventType, value1 = From, value2 = To)

edges <- data %>% filter(type == "TaskPrnt") %>% select(value1, value2)
vertices <- data %>% filter(type != "TaskPrnt") %>% spread(type, time) %>%
  transmute(name = value1, start = `TaskStrt`, end = `TaskEnd `, len = end - start) %>%
  bind_rows(c(name = 0))

vnames <- unique(vertices$name)
enames <- unique(c(edges$value1, edges$value2))
# print(enames[!(enames %in% vnames)])

vertices <- full_join(vertices, tibble(name = enames))

g <- graph_from_data_frame(edges, vertices = vertices)

V(g)$level = -c(distances(g, "0", weights = rep(-1, length.out = length(E(g))), mode = "out"))
vs <- igraph::as_data_frame(g, what = "vertices")
print(max(vs$level))
print(max(vs %>% group_by(level) %>% summarise(n = n())))

#ggplot(vs, aes(x = level)) +
#  geom_bar() + coord_cartesian(ylim=c(0, 100))

to.ms <- function(ns) {
  ns / 1000 / 1000
}

vs %>%
  ggplot(aes(x = level)) +# coord_cartesian(ylim=c(0, 20)) +
  geom_linerange(aes(
    ymin = if_else(is.na(start), -1, to.ms(start - min(start, na.rm = T))/1000),
    ymax = if_else(is.na(end), 0, to.ms(end - min(start, na.rm = T))/1000),
    #size = if_else(is.na(len), 1000, to.ms(len)),
    color = is.na(len)),
    alpha = 0.8,
    size = 1
  ) +
  geom_point(aes(
    y = if_else(is.na(start), -1, to.ms(start - min(start, na.rm = T))/100),
    color = is.na(len)),
    alpha = 0.8,
    size = 1
  )
  #+ geom_bar(aes(fill = factor(is.na(len))), position = position_stack(), width = 1)

vs %>% group_by(level) %>% summarise(n = n()) %>%
  ggplot(aes(x = level, y = n)) + geom_line()
