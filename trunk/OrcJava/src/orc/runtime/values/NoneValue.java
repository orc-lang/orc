package orc.runtime.values;

/* Common ancestor for the option values */

public class NoneValue extends OptionValue { 
	@Override
	public boolean isNone() { return true; }
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

