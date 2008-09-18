package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import orc.lib.date.DateRange;
import orc.lib.date.DateRanges;

import org.joda.time.LocalDateTime;

@SuppressWarnings("deprecation")
public class DateRangesField extends Field<DateRanges> {
	private DateRange span;
	private int minHour;
	private int maxHour;
	private static String[] daysOfWeek = {"", "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
	
	public DateRangesField(String key, String label, DateRange span, int minHour, int maxHour) {
		super(key, label, new DateRanges());
		this.span = span;
		this.minHour = minHour;
		this.maxHour = maxHour;
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<table cellspacing='0' class='DateRangesField'>");
		renderHeader(out);
		for (int hour = minHour; hour < maxHour; ++hour) {
			renderHour(out, hour);
		}
		out.write("</table>");
	}
	
	private void renderTime(PrintWriter out, LocalDateTime date) throws IOException {
		out.write("<input type='checkbox'" +
				" name='" + key + "'" +
				" value='" + toTimeID(date) + "'" +
				(value.spans(date) ? " checked" : "") +
				">");
	}
	
	private static String toTimeID(LocalDateTime date) {
		return date.getYear() +
			"_" + date.getMonthOfYear() +
			"_" + date.getDayOfMonth() +
			"_" + date.getHourOfDay();
	}
	
	private static DateRange fromTimeID(String timeID) {
		String[] parts = timeID.split("_");
		if (parts.length != 4) return DateRange.NULL;
		try {
			LocalDateTime start = new LocalDateTime(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3]),
					0, 0, 0);
			LocalDateTime end = start.plusHours(1);
			return new DateRange(start, end);
		} catch (NumberFormatException _) {
			return DateRange.NULL;
		}
	}
	
	private void renderHour(PrintWriter out, int hour) throws IOException {
		out.write("<tr>");
		out.write("<th>");
		out.write(formatHour(hour));
		out.write("</th>");
		LocalDateTime current = span.start.withHourOfDay(hour);
		LocalDateTime end = span.end.withHourOfDay(hour);
		while (current.compareTo(end) < 0) {
			out.write("<td>");
			renderTime(out, current);
			out.write("</td>");
			current = current.plusDays(1);
		}
		out.write("</tr>");
	}
	
	private void renderHeader(PrintWriter out) throws IOException {
		out.write("<tr><th>&nbsp;</th>");
		LocalDateTime current = span.start;
		LocalDateTime end = span.end.withHourOfDay(0);
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
	
	private String formatDateHeader(LocalDateTime date) {
		return daysOfWeek[date.getDayOfWeek()] +
			" " + date.getMonthOfYear() +
			"/" + date.getDayOfMonth();
	}
	
	private void readTimeIDs(String[] timeIDs) {
		value = new DateRanges();
		if (timeIDs == null) return;
		for (String timeID : timeIDs) {
			DateRange range = fromTimeID(timeID);
			value.union(new DateRanges(range));
		}
	}

	public void readRequest(FormData request, List<String> errors) {
		readTimeIDs(request.getParameterValues(key));
	}
}
