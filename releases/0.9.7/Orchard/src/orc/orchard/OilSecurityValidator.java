package orc.orchard;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.Walker;
import orc.ast.oil.arg.Site;
import orc.error.Located;
import orc.error.SourceLocation;

/**
 * Check an OIL expression for security violations.
 * @author quark
 */
public class OilSecurityValidator extends Walker {
	private boolean hasProblems = false;
	private List<SecurityProblem> problems = new LinkedList<SecurityProblem>();
	public boolean hasProblems() {	return hasProblems; }
	public List<SecurityProblem> getProblems() { return problems; }
	
	private static Set<String> allowedClasses;
	static {
		allowedClasses = new HashSet<String>(); // a Trie might be more efficient if it were standard
		
		// java.lang
		allowedClasses.add("java.lang.StrictMath");
		allowedClasses.add("java.lang.Math");
		allowedClasses.add("java.lang.Boolean");
		allowedClasses.add("java.lang.Byte");
		allowedClasses.add("java.lang.Character");
		allowedClasses.add("java.lang.Short");
		allowedClasses.add("java.lang.Integer");
		allowedClasses.add("java.lang.Long");
		allowedClasses.add("java.lang.Double");
		allowedClasses.add("java.lang.Float");
		allowedClasses.add("java.lang.String");
		allowedClasses.add("java.lang.StringBuffer");
		allowedClasses.add("java.lang.StringBuilder");
		
		// java.util
		allowedClasses.add("java.util.ArrayList");
		allowedClasses.add("java.util.Arrays");
		allowedClasses.add("java.util.BitSet");
		allowedClasses.add("java.util.Collections");
		allowedClasses.add("java.util.Currency");
		allowedClasses.add("java.util.HashMap");
		allowedClasses.add("java.util.HashSet");
		allowedClasses.add("java.util.Hashtable");
		allowedClasses.add("java.util.IdentityHashMap");
		allowedClasses.add("java.util.LinkedHashMap");
		allowedClasses.add("java.util.LinkedHashSet");
		allowedClasses.add("java.util.LinkedList");
		allowedClasses.add("java.util.PriorityQueue");
		allowedClasses.add("java.util.Random");
		allowedClasses.add("java.util.Stack");
		allowedClasses.add("java.util.TreeMap");
		allowedClasses.add("java.util.TreeSet");
		allowedClasses.add("java.util.Vector");
		
		// orc.lib
		allowedClasses.add("orc.lib.state.Set");
		allowedClasses.add("orc.lib.state.Map");
		allowedClasses.add("orc.lib.net.Upcoming");
		allowedClasses.add("orc.lib.net.Geocoder");
		allowedClasses.add("orc.lib.net.GoogleCalendar");
		allowedClasses.add("orc.lib.net.NOAAWeather");
		allowedClasses.add("orc.lib.date.DateTimeRange");
		
		// orc.lib.orchard
		allowedClasses.add("orc.lib.orchard.forms.Form");
		allowedClasses.add("orc.lib.orchard.forms.Textbox");
		allowedClasses.add("orc.lib.orchard.forms.Textarea");
		allowedClasses.add("orc.lib.orchard.forms.Checkbox");
		allowedClasses.add("orc.lib.orchard.forms.Button");
		allowedClasses.add("orc.lib.orchard.forms.IntegerField");
		allowedClasses.add("orc.lib.orchard.forms.UploadField");
		allowedClasses.add("orc.lib.orchard.forms.FormInstructions");
		allowedClasses.add("orc.lib.orchard.forms.PasswordField");
		allowedClasses.add("orc.lib.orchard.forms.Mandatory");
		allowedClasses.add("orc.lib.orchard.forms.FieldGroup");
		allowedClasses.add("orc.lib.orchard.forms.DateField");
		allowedClasses.add("orc.lib.orchard.forms.DateTimeRangesField");
		
		// org.joda.time
		allowedClasses.add("org.joda.time.format.DateTimeFormat");
		allowedClasses.add("org.joda.time.DateTime");
		allowedClasses.add("org.joda.time.LocalDateTime");
		allowedClasses.add("org.joda.time.LocalDate");

	}
	
	public static class SecurityProblem implements Located {
		private String message;
		private SourceLocation location;
		public SecurityProblem(String message, SourceLocation location) {
			this.message = message;
			this.location = location;
		}
		public String getMessage() {
			return message;
		}
		public SourceLocation getSourceLocation() {
			return location;
		}
		public String toString() {
			if (location != null) {
				return message + " at " + location;
			} else return message;
		}
	}
	
	@Override
	public void enter(Site site) {
		String protocol = site.site.getProtocol();
		String location = site.site.getLocation().toString();
		if (protocol.equals(orc.ast.sites.Site.JAVA)) {
			// only whitelisted Java classes are allowed
			if (!allowedClasses.contains(location)) {
				hasProblems = true;
				// FIXME: once we have source location information, use it
				problems.add(new SecurityProblem(
						"Site URL '"+location+"' not" +
						" allowed.",
						null));
			}
		} else if (protocol.equals(orc.ast.sites.Site.ORC)) {
			// all Orc sites are allowed
		} else {
			hasProblems = true;
			// FIXME: once we have source location information, use it
			problems.add(new SecurityProblem(
					"Site protocol '"+protocol+"' not" +
					" allowed.",
					null));
		}
	}
}