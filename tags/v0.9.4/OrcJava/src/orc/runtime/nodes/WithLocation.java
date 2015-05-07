package orc.runtime.nodes;

import orc.error.Located;
import orc.error.SourceLocation;
import orc.runtime.Token;

/**
 * Annotates a node with a source location which is tracked by the
 * token (for use in tracing and error messages). I opted to introduce
 * a new node rather than add this capability for existing nodes because
 * that makes it easier to omit this information in an "optimized" build.
 * 
 * @author quark
 */
public class WithLocation extends Node implements Located {
	private final Node next;
	private final SourceLocation location;
	public WithLocation(final Node next, final SourceLocation location) {
		super();
		this.next = next;
		this.location = location;
	}
	@Override
	public void process(Token t) {
		t.setSourceLocation(location);
		next.process(t);
	}
	public SourceLocation getSourceLocation() {
		return location;
	}
}
