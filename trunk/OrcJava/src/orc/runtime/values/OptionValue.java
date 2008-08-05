package orc.runtime.values;

/* Common ancestor for the option values */

public abstract class OptionValue<V extends Value> extends Value {
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
