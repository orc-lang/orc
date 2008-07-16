package orc.runtime.values;

/* Common ancestor for the option values */

public class SomeValue extends OptionValue { 
	
	public Value content;
	
	public SomeValue(Value content) {
		this.content = content;
	}
	
	public boolean isSome() { return true; }
	
	public Value untag() { return content; }
	
}
