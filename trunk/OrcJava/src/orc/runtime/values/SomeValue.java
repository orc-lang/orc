package orc.runtime.values;

import orc.runtime.sites.core.Equal;

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

	public boolean equivalentTo(Object that) {
		return (that instanceof SomeValue)
			&& Equal.equivalent(content, ((SomeValue)that).content);
	}
}
