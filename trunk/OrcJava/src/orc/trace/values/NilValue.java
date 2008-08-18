package orc.trace.values;

import orc.trace.query.Frame;
import orc.trace.query.Term;

public class NilValue extends ListValue {
	public final static NilValue singleton = new NilValue();
	private NilValue() {}
}
