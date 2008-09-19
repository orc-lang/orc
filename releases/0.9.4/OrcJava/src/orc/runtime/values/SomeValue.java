package orc.runtime.values;

/* Common ancestor for the option values */

public class SomeValue extends OptionValue { 
	public Object content;
	
	public SomeValue(Object content) {
		this.content = content;
	}
	
	@Override
	public boolean isSome() { return true; }
	
	@Override
	public Object untag() { return content; }
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
