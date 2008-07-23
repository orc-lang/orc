package orc.orchard;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.Walker;
import orc.ast.oil.arg.Site;
import orc.error.Debuggable;
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
	
	public static class SecurityProblem implements Debuggable {
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
		if (!protocol.equals(orc.ast.sites.Site.ORC)) {
			hasProblems = true;
			// FIXME: once we have source location information, use it
			problems.add(new SecurityProblem(
					"Site protocol '"+protocol+"' not" +
					" allowed.",
					null));
		}
	}
}