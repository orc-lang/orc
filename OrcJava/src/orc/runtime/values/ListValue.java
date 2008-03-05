package orc.runtime.values;

import java.util.List;

/* Common ancestor for the cons values */

public abstract class ListValue extends Value { 
	
	public abstract List<Value> enlist();
	
}
