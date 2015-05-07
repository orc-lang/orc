package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import orc.lib.date.DateTimeRange;
import orc.lib.date.DateTimeRanges;

import org.joda.time.DateTime;

@SuppressWarnings("deprecation")
public class DateTimeRangesField extends Field<DateTimeRanges> {
	private DateTimeRange span;
	private int minHour;
	private int maxHour;
	private static String[] daysOfWeek = {"", "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
	
	public DateTimeRangesField(String key, String label, DateTimeRange span, int minHour, int maxHour) {
		super(key, label, new DateTimeRanges());
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
	
	private static DateTimeRange fromTimeID(String timeID) {
		String[] parts = timeID.split("_");
		if (parts.length != 4) return DateTimeRange.NULL;
		try {
			DateTime start = new DateTime(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3]),
					0, 0, 0);
			DateTime end = start.plusHours(1);
			return new DateTimeRange(start, end);
		} catch (NumberFormatException _) {
			return DateTimeRange.NULL;
		}
	}
	
	private void renderHour(PrintWriter out, int hour) throws IOException {
		out.write("<tr>");
		out.write("<th>");
		out.write(formatHour(hour));
		out.write("</th>");
		DateTime current = span.start.withHourOfDay(hour);
		DateTime end = span.end.withHourOfDay(hour);
		while (current.compareTo(end) < 0) {
			out.write("<td>");
			renderTime(out, current);
			out.write("</td>");
			current = current.plusDays(1);
		}
		out.write("</tr>");
	}
	
	private void renderTableHeader(PrintWriter out) throws IOException {
		out.write("<tr><th>&nbsp;</th>");
		DateTime current = span.start;
		DateTime end = span.end.withHourOfDay(0);
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
		} else if (hour > 12) {
			return (hour % 12) + "pm";
		} else {
			return hour + "am";
		}
	}
	
	private String formatDateHeader(DateTime date) {
		return daysOfWeek[date.getDayOfWeek()] +
			" " + date.getMonthOfYear() +
			"/" + date.getDayOfMonth();
	}
	
	private void readTimeIDs(String[] timeIDs) {
		value = new DateTimeRanges();
		if (timeIDs == null) return;
		for (String timeID : timeIDs) {
			DateTimeRange range = fromTimeID(timeID);
			value.union(new DateTimeRanges(range));
		}
	}

	public void readRequest(FormData request, List<String> errors) {
		readTimeIDs(request.getParameterValues(key));
	}
}
