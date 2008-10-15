package orc.runtime.values;

/* Common ancestor for the option values */

public abstract class OptionValue extends Value implements Immutable {
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
