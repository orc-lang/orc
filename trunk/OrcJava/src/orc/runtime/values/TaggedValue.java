package orc.runtime.values;

import orc.runtime.sites.Site;
import orc.runtime.sites.core.Datasite;

public class TaggedValue extends Value {

	public Object payload;
	public Datasite tag; // typically this is a reference to the injection site itself
	
	public TaggedValue(Object object, Datasite tag) {
		this.payload = object;
		this.tag = tag;
	}
	
	public String toString() {

		if (payload instanceof TupleValue) {
			TupleValue tup = (TupleValue)payload;
			
			if (tup.size() == 0) {
				return tag.tagname + "()";
			}
			else if (tup.size() > 1) {
				return tag.tagname + tup.toString();
			}			
		}
		return tag.tagname + "(" + payload + ")";
	}
	
}
