package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import orc.lib.state.Interval;
import orc.lib.state.Intervals;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

@SuppressWarnings("deprecation")
public class DateTimeRangesField extends Field<Intervals<DateTime>> {
	private Interval<LocalDate> span;
	private int minHour;
	private int maxHour;
	private static String[] daysOfWeek = {"", "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
	
	public DateTimeRangesField(String key, String label, Interval<LocalDate> span, int minHour, int maxHour) {
		super(key, label, new Intervals<DateTime>());
		this.span = span;
		this.minHour = minHour;
		this.maxHour = maxHour;
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<table cellspacing='0' class='DateTimeRangesField'>");
		renderTableHeader(out);
		for (int hour = minHour; hour < maxHour; ++hour) {
			renderHour(out, hour);
		}
		out.write("</table>");
	}
	
	private void renderTime(PrintWriter out, DateTime date) throws IOException {
		out.write("<input type='checkbox'" +
				" name='" + key + "'" +
				" value='" + toTimeID(date) + "'" +
				(value.spans(date) ? " checked" : "") +
				">");
	}
	
	private static String toTimeID(DateTime date) {
		return date.getYear() +
			"_" + date.getMonthOfYear() +
			"_" + date.getDayOfMonth() +
			"_" + date.getHourOfDay();
	}
	
	private static Interval<DateTime> fromTimeID(String timeID) {
		String[] parts = timeID.split("_");
		if (parts.length != 4) return new Interval(new DateTime());
		try {
			DateTime start = new DateTime(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3]),
					0, 0, 0);
			DateTime end = start.plusHours(1);
			return new Interval<DateTime>(start, end);
		} catch (NumberFormatException _) {
			return new Interval(new DateTime());
		}
	}
	
	private void renderHour(PrintWriter out, int hour) throws IOException {
		out.write("<tr>");
		out.write("<th>");
		out.write(formatHour(hour));
		out.write("</th>");
		LocalDate current = span.getStart();
		LocalDate end = span.getEnd();
		LocalTime time = new LocalTime(hour, 0);
		while (current.compareTo(end) < 0) {
			out.write("<td>");
			renderTime(out, current.toDateTime(time));
			out.write("</td>");
			current = current.plusDays(1);
		}
		out.write("</tr>");
	}
	
	private void renderTableHeader(PrintWriter out) throws IOException {
		out.write("<tr><th>&nbsp;</th>");
		LocalDate current = span.getStart();
		LocalDate end = span.getEnd();
		while (current.compareTo(end) < 0) {
			out.write("<th>");
			out.write(formatDateHeader(current));
			out.write("</th>");
			current = current.plusDays(1);
		}
		out.write("</tr>");
	}
	
	private String formatHour(int hour) {
		if (hour == 0) {
			return "12am";
		} else if (hour == 12) {
			return "12pm";
		} else if (hour > 12) {
			return (hour % 12) + "pm";
		} else {
			return hour + "am";
		}
	}
	
	private String formatDateHeader(LocalDate date) {
		return daysOfWeek[date.getDayOfWeek()] +
			" " + date.getMonthOfYear() +
			"/" + date.getDayOfMonth();
	}
	
	private void readTimeIDs(String[] timeIDs) {
		value = new Intervals();
		if (timeIDs == null) return;
		// union ranges starting at the end for efficiency
		for (int i = timeIDs.length-1; i >= 0; --i) {
			Interval<DateTime> range = fromTimeID(timeIDs[i]);
			value = value.union(range);
		}
	}

	public void readRequest(FormData request, List<String> errors) {
		readTimeIDs(request.getParameterValues(key));
	}
}
