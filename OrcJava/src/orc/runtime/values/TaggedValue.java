package orc.runtime.values;

import java.util.Arrays;

import orc.runtime.sites.Site;
import orc.runtime.sites.core.Datasite;

public class TaggedValue extends Value {
	public Object[] values;
	public Datasite tag; // typically this is a reference to the injection site itself
	
	public TaggedValue(Datasite tag, Object[] values) {
		this.values = values;
		this.tag = tag;
	}
	
	public String toString() {
		return tag.tagName + Arrays.toString(values);
	}
}
