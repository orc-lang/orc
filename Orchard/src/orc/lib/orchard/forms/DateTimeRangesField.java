//
// DateTimeRangesField.java -- Java class DateTimeRangesField
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import orc.lib.state.Interval;
import orc.lib.state.Intervals;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

public class DateTimeRangesField extends Field<Intervals<DateTime>> {
    private final Interval<LocalDate> span;
    private final int minHour;
    private final int maxHour;
    private static String[] daysOfWeek = { "", "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su" };

    public DateTimeRangesField(final String key, final String label, final Interval<LocalDate> span, final int minHour, final int maxHour) {
        super(key, label, new Intervals<DateTime>());
        this.span = span;
        this.minHour = minHour;
        this.maxHour = maxHour;
    }

    @Override
    public void renderControl(final PrintWriter out) throws IOException {
        out.write("<table cellspacing='0' class='DateTimeRangesField'>");
        renderTableHeader(out);
        for (int hour = minHour; hour < maxHour; ++hour) {
            renderHour(out, hour);
        }
        out.write("</table>");
    }

    private void renderTime(final PrintWriter out, final DateTime date) throws IOException {
        out.write("<input type='checkbox'" + " name='" + key + "'" + " value='" + toTimeID(date) + "'" + (value.spans(date) ? " checked" : "") + ">");
    }

    private static String toTimeID(final DateTime date) {
        return date.getYear() + "_" + date.getMonthOfYear() + "_" + date.getDayOfMonth() + "_" + date.getHourOfDay();
    }

    private static Interval<DateTime> fromTimeID(final String timeID) {
        final String[] parts = timeID.split("_");
        if (parts.length != 4) {
            return new Interval(new DateTime());
        }
        try {
            final DateTime start = new DateTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), 0, 0, 0);
            final DateTime end = start.plusHours(1);
            return new Interval<DateTime>(start, end);
        } catch (final NumberFormatException nfe) {
            return new Interval(new DateTime());
        }
    }

    private void renderHour(final PrintWriter out, final int hour) throws IOException {
        out.write("<tr>");
        out.write("<th>");
        out.write(formatHour(hour));
        out.write("</th>");
        LocalDate current = span.getStart();
        final LocalDate end = span.getEnd();
        final LocalTime time = new LocalTime(hour, 0);
        while (current.compareTo(end) < 0) {
            out.write("<td>");
            renderTime(out, current.toDateTime(time));
            out.write("</td>");
            current = current.plusDays(1);
        }
        out.write("</tr>");
    }

    private void renderTableHeader(final PrintWriter out) throws IOException {
        out.write("<tr><th>&nbsp;</th>");
        LocalDate current = span.getStart();
        final LocalDate end = span.getEnd();
        while (current.compareTo(end) < 0) {
            out.write("<th>");
            out.write(formatDateHeader(current));
            out.write("</th>");
            current = current.plusDays(1);
        }
        out.write("</tr>");
    }

    private String formatHour(final int hour) {
        if (hour == 0) {
            return "12am";
        } else if (hour == 12) {
            return "12pm";
        } else if (hour > 12) {
            return hour % 12 + "pm";
        } else {
            return hour + "am";
        }
    }

    private String formatDateHeader(final LocalDate date) {
        return daysOfWeek[date.getDayOfWeek()] + " " + date.getMonthOfYear() + "/" + date.getDayOfMonth();
    }

    private void readTimeIDs(final String[] timeIDs) {
        value = new Intervals();
        if (timeIDs == null) {
            return;
        }
        // union ranges starting at the end for efficiency
        for (int i = timeIDs.length - 1; i >= 0; --i) {
            final Interval<DateTime> range = fromTimeID(timeIDs[i]);
            value = value.union(range);
        }
    }

    @Override
    public void readRequest(final FormData request, final List<String> errors) {
        readTimeIDs(request.getParameterValues(key));
    }
}
