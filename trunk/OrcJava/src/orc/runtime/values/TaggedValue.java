package orc.runtime.values;

import orc.runtime.sites.Site;
import orc.runtime.sites.core.Datasite;

public class TaggedValue extends Value {
	public TupleValue payload;
	public Datasite tag; // typically this is a reference to the injection site itself
	
	public TaggedValue(TupleValue payload, Datasite tag) {
		this.payload = payload;
		this.tag = tag;
	}
	
	public String toString() {
		return tag.tagname + payload;
	}
}
