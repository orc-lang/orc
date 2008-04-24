package orc.runtime.values;

import java.util.List;

/* Common ancestor for the cons values */

public abstract class ListValue extends Value { 
	
	public abstract List<Value> enlist();
	
	public static ListValue make(List<Value> vs) {
		
		ListValue l = new NilValue();
		
		for(int i = vs.size() - 1; i >= 0; i--) {
			l = new ConsValue(vs.get(i), l);
		}
		
		return l;
	}
}
