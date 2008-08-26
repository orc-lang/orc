package orc.trace.values;

import orc.trace.Terms;

public abstract class AbstractValue implements Value {
	public String toString() {
		return Terms.printToString(this);
	}
}
