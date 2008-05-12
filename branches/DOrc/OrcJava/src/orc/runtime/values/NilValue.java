package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

public class NilValue extends ListValue {
	private static final long serialVersionUID = 1L;
	public boolean isNil() { return true; }

	public String toString() { return "[]"; }

	@Override
	public List<Value> enlist() {
		
		return new LinkedList<Value>();
	}
	
	
}
