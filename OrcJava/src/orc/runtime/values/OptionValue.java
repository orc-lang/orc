package orc.runtime.values;

/* Common ancestor for the option values */

public abstract class OptionValue extends Value {
	@Override
	public orc.orchard.oil.Value marshal() {
		return new orc.orchard.oil.Option(isNone() ? null : untag().marshal());
	}
}
