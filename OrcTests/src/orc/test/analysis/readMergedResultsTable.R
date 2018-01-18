# Utilities for reading results formatted using jthywiss' experiment infrastructure.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

# TODO: This should be a package. But that's a pain so for now just use source("...") or something similar.

if(!exists("scriptDir"))
  scriptDir <- dirname(sys.frame(1)$ofile)

# The file 'config.R' in the same directory as this script should contain
# a line "orcRepoPath = ..." which sets the path to the orc repository check
# out. This is needed to call orc.test.util.RunResultsTableMerge.

source(file.path(scriptDir, "config.R"))

if (!exists("orcRepoPath")) {
  stop("The file 'config.R' in ", scriptDir, " should set the variable orcRepoPath.")
}

simpleCapitalize <- function(x) {
  s <- strsplit(x, " ")[[1]]
  paste(toupper(substring(s, 1,1)), substring(s, 2),
        sep="", collapse=" ")
}

cleanColumnName <- function(name) {
  if (gregexpr("\\[.*\\]", name) != -1) {
    sub(pattern = ".*\\[(.*)\\].*", replacement = "\\1", name)
  } else {
    gsub(pattern = "\\(.*\\)|[[:space:]]", replacement = "", simpleCapitalize(as.character(name)))
  }
}

# Load the merged CSV for the given runDirectory and fileBaseName.
#
# This will automatically run orc.test.util.RunResultsTableMerge as needed.
# If the merged CSV is already available it will not run it unless invalidate
# is TRUE.
#
# The returned value will have the columns named based on the ids of the
# factors if they are available. If there is no "[...]" block with the id
# in a column name this will attempt to make something more or less correct.
readMergedResultsTable <- function(runDirectory, fileBaseName, invalidate = FALSE) {
  outputDir <- file.path(runDirectory, "merged")
  if (!dir.exists(outputDir)) {
    dir.create(outputDir)
  }
  outputFile <-  file.path(outputDir, sprintf("%s.csv", fileBaseName))

  if (invalidate || !file.exists(outputFile) || file.size(outputFile) == 0) {
    print(paste("Merging raw-output files into", outputFile), quote = FALSE)

    orcTestPath <- file.path(orcRepoPath, "OrcTests", "build")
    scalaLibraryPath <- file.path(orcRepoPath, "OrcScala", "lib", "scala-library.jar")
    if (!file.exists(scalaLibraryPath) || !dir.exists(orcTestPath)) {
      stop("Count not find a required file: ", scalaLibraryPath, " OR ", orcTestPath)
    }
    classPath <- file.path(orcTestPath, scalaLibraryPath, fsep = .Platform$path.sep)
    system2("java",
            c("-classpath", classPath, "orc.test.util.RunResultsTableMerge", runDirectory, fileBaseName),
            stdout = outputFile, wait = TRUE)
  } else {
    print(paste("Reading already computed data from", outputFile), quote = FALSE)
  }

  if (file.size(outputFile) == 0) {
    stop("Merge failed. Check runDirectory (", runDirectory,") and fileBaseName (", fileBaseName, ").")
  }

  header <- read.csv(outputFile, header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  res <- read.csv(outputFile, col.names = names)
  res
}

readResultsTable <- function(fileName) {
  header <- read.csv(fileName, header = FALSE, nrows = 1)
  names <- vapply(header[1,], cleanColumnName, "")
  res <- read.csv(fileName, col.names = names)
  res
}
