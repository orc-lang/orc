package orc.runtime.values;

/* Common ancestor for the option values */

public class SomeValue extends OptionValue { 
	public Object content;
	
	public SomeValue(Object content) {
		this.content = content;
	}
	
	public boolean isSome() { return true; }
	
	public Object untag() { return content; }
}
