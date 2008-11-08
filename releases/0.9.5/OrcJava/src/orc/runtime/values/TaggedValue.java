package orc.runtime.values;

import java.util.Arrays;

import orc.runtime.sites.Site;
import orc.runtime.sites.core.Datasite;
import orc.runtime.sites.core.Equal;

public class TaggedValue extends Value implements Eq {
	public Object[] values;
	public Datasite tag; // typically this is a reference to the injection site itself
	
	public TaggedValue(Datasite tag, Object[] values) {
		this.values = values;
		this.tag = tag;
	}
	
	public String toString() {
		return tag.tagName + Arrays.toString(values);
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public boolean equals(Object that_) {
		if (that_ == null) return false;
		return eqTo(that_);
	}
	public boolean eqTo(Object that_) {
		if (!(that_ instanceof TaggedValue)) return false;
		TaggedValue that = (TaggedValue)that_;
		if (that.tag != this.tag) return false;
		if (that.values.length != this.values.length) return false;
		for (int i = 0; i < this.values.length; ++i) {
			if (!Equal.eq(this.values[i], that.values[i])) return false;
		}
		return true;
	}
}
