package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

public class DateField extends SingleField<LocalDate> {

	public DateField(String key, String label) {
		super(key, label, "");
	}
	
	@Override
	public void renderHeader(PrintWriter out, Set<String> flags) throws IOException {
		if (flags.contains("DateField")) return;
		flags.add("DateField");
		out.write(
			"<link REL=\"STYLESHEET\" TYPE=\"text/css\" HREF=\"cal/calendar-blue.css\" />"
			+ "<script type=\"text/javascript\" src=\"cal/calendar.js\"></script>"
			+ "<script type=\"text/javascript\" src=\"cal/lang/calendar-en.js\"></script>"
			+ "<script type=\"text/javascript\" src=\"cal/calendar-setup.js\"></script>"
			+ "<script type=\"text/javascript\" src=\"date.js\"></script>"
			+ "<script type=\"text/javascript\">"
			+ "function autoformat_date_input(field, format) {"
			+ "	field.value = net_sixfingeredman_date.format(net_sixfingeredman_date.fromHumanDate(field.value), format);"
			+ "}"
			+ "</script>");
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='textbox'" +
				" size='13'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" value='" + posted + "'" +
				" onchange='autoformat_date_input(this, \"m/d/Y\")'" +
				">&#160;<img id='" + key + "_button' width='19' height='15' style='cursor: pointer' src='calendar.gif'/>" +
				"<script type='text/javascript'>" +
				"Calendar.setup({" +
				"  inputField  : '"+key+"'," +
				"  button      : '"+key+"_button'" +
				"});" +
				"</script>");
	}

	@Override
	public LocalDate requestToValue(String posted) throws ValidationException {
		try {
			if (posted.equals("")) return null;
			return DateTimeFormat.forPattern("MM/dd/yyyy").parseDateTime(posted).toLocalDate();
		} catch (IllegalArgumentException e) {
			throw new ValidationException(label + " must match the format MM/DD/YYYY.");
		}
	}
}
