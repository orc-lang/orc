package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import orc.lib.orchard.calendar.DateRange;
import orc.lib.orchard.calendar.DateRanges;

@SuppressWarnings("deprecation")
public class DateRangesField extends Field<DateRanges> {
	private DateRange span;
	private int minHour;
	private int maxHour;
	private static String[] daysOfWeek = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
	
	public DateRangesField(String key, String label, DateRange span, int minHour, int maxHour) {
		super(key, label, new DateRanges());
		this.span = span;
		this.minHour = minHour;
		this.maxHour = maxHour;
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<table class='DateRangesField'>");
		renderHeader(out);
		for (int hour = minHour; hour < maxHour; ++hour) {
			renderHour(out, hour);
		}
		out.write("</table>");
	}
	
	private void renderTime(PrintWriter out, Date date) throws IOException {
		out.write("<input type='checkbox'" +
				" name='" + key + "'" +
				" value='" + toTimeID(date) + "'" +
				(value.spans(date) ? " checked" : "") +
				">");
	}
	
	private static String toTimeID(Date date) {
		return date.getYear() +
			"_" + date.getMonth() +
			"_" + date.getDate() +
			"_" + date.getHours();
	}
	
	private static DateRange fromTimeID(String timeID) {
		String[] parts = timeID.split("_");
		if (parts.length != 4) return DateRange.NULL;
		try {
			Date start = new Date(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3]),
					0);
			Date end = (Date)start.clone();
			if (start.getHours() == 23) {
				end.setHours(0);
				end.setDate(start.getDate()+1);
			} else {
				end.setHours(start.getHours()+1);
			}
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
		Date current = (Date)span.start.clone();
		current.setHours(hour);
		Date end = (Date)span.end.clone();
		end.setHours(hour);
		while (current.compareTo(end) < 0) {
			out.write("<td>");
			renderTime(out, current);
			out.write("</td>");
			current.setDate(current.getDate()+1);
		}
		out.write("</tr>");
	}
	
	private void renderHeader(PrintWriter out) throws IOException {
		out.write("<tr><th>&nbsp;</th>");
		Date current = (Date)span.start.clone();
		Date end = (Date)span.end.clone();
		end.setHours(0);
		while (current.compareTo(end) < 0) {
			out.write("<th>");
			out.write(formatDateHeader(current));
			out.write("</th>");
			current.setDate(current.getDate()+1);
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
	
	private String formatDateHeader(Date date) {
		return daysOfWeek[date.getDay()] +
			" " + (date.getMonth()+1) +
			"/" + date.getDate();
	}
	
	private void readTimeIDs(String[] timeIDs) {
		value = new DateRanges();
		for (String timeID : timeIDs) {
			DateRange range = fromTimeID(timeID);
			value.union(new DateRanges(range));
		}
	}

	public void readRequest(HttpServletRequest request, List<String> errors) {
		readTimeIDs(request.getParameterValues(key));
	}
}
