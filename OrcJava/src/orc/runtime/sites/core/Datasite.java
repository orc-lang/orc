package orc.runtime.sites.core;

import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.Constructor;
import orc.runtime.values.TaggedValue;

public class Datasite extends Constructor {

	int arity;
	public String tagname;
	
	public Datasite(int arity, String tagname) {
		this.arity = arity;
		this.tagname = tagname;
	}
	
	@Override
	public Object construct(Args args) throws TokenException {
		
		if (args.size() != arity) 
			{ throw new ArityMismatchException(arity, args.size()); }
		
		return new TaggedValue(args.condense(), this);
	}

	@Override
	public Object deconstruct(Object arg) throws TokenException {
		
		if (arg instanceof TaggedValue) {
			TaggedValue v = (TaggedValue)arg;
			if (v.tag == this) {
				return v.payload;
			}
		}
		return null;
	}
	
	public String toString() {
		return tagname + "/" + arity;
	}

}
