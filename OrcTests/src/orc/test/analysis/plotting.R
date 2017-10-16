# Utilities for plotting performance data
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

library(ggplot2)

# Create a geom_col (bar chart) plot with error bars.
#
# Required aesthetics: x (should be a factor), y, ymin, ymax
# Recommended aesthetics: fill
# Discouraged aesthetics: color (it makes the bars and error bars look odd)
#
# Additional arguments are forwarded to both geom_col and geom_errorbar.
#
geom_col_errorbar <- function (mapping = NULL, ...) {
  list(
    geom_col(mapping, position = position_dodge(), ...),
    geom_errorbar(mapping, position = position_dodge(), alpha = 0.3, ...)
  )
}

# TODO:
# geom_col_values <- function (mapping = NULL, ylim, label, position, ...) {
#   yMin <- ylim[1]
#   yMax <- ylim[2]
#   .geom_col_values.computePos <<- function(v) {
#     pmax(pmin(v + (0.01 * yMax), yMax * 0.9), 0)
#   }
#   list(
#     geom_text(aes_(label = bquote(format(.(label), digits = 2, nsmall=0)), y = bquote(.geom_col_values.computePos(.(position)))), position = position_dodge(0.9), vjust = 0),
#     coord_cartesian(ylim = ylim)
#   )
# }


# geom_hline(yintercept = 1, alpha = 0.5, color = "blue")

# Create a geom_line (X/Y plot) plot with error lines.
#
# Required aesthetics: x (should be continous), y, ymin, ymax
# Recommended aesthetics: color
#
# break_from can be provided and is passed on to scale_x_continuous_breaks_from.
# position can be used to override the position of the error lines. The default is a small horizontal offset.
#
# Additional arguments are forwarded to both geom_line and geom_linerange.
#
geom_line_linerange <- function (mapping = NULL, position = position_dodge(0.2), ..., breaks_from = NULL) {
  list(
    geom_line(mapping, ...),
    geom_linerange(mapping, alpha = 0.5, position = position, ...)
  )
}

# Set the X-axis breaks to be the values that appear in breaks_from.
#
# Additional arguments are forwarded to scale_x_continuous.
#
scale_x_continuous_breaks_from <- function(..., breaks_from = NULL) {
  if (!is.null(breaks_from)) {
    breaks <- as.numeric(levels(factor(breaks_from)))
  } else {
    breaks <- waiver()
  }
  scale_x_continuous(breaks = breaks, minor_breaks = NULL, ...)
}

