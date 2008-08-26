package orc.trace.values;


public class NilValue extends ListValue {
	public final static NilValue singleton = new NilValue();
	private NilValue() {}
}
