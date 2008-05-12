package orc.runtime.values;

/* Common ancestor for the option values */

public class SomeValue extends OptionValue { 
	private static final long serialVersionUID = 1L;
	public Value content;
	
	public SomeValue(Value content) {
		this.content = content;
	}
	
	public boolean isSome() { return true; }
	
	public Value untag() { return content; }
	
}
