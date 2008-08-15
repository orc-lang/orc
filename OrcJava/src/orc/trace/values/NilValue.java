package orc.trace.values;

import java.io.Serializable;

public class NilValue extends ListValue {
	public final static NilValue singleton = new NilValue();
	private NilValue() {}
}
