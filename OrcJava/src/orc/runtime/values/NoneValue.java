package orc.runtime.values;

/* Common ancestor for the option values */

public class NoneValue<V extends Value> extends OptionValue<V> { 
	
	public boolean isNone() { return true; }
	
}

