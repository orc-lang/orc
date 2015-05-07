package orc.runtime.sites.core;

import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.TaggedValue;
import orc.runtime.values.TupleValue;

public final class Datasite extends DotSite {

	int arity;
	public String tagName;
	
	public Datasite(int arity, String tagname) {
		this.arity = arity;
		this.tagName = tagname;
	}
	
	@Override
	protected void addMembers() {
		addMember("?", new PartialSite() {
			@Override
			public TupleValue evaluate(Args args) throws TokenException {
				return deconstruct(args.getArg(0));
			}
		});
	}
	
	@Override
	protected void defaultTo(Args args, Token token) throws TokenException {
		if (args.size() != arity) 
			{ throw new ArityMismatchException(arity, args.size()); }
		
		token.resume(new TaggedValue(this, args.asArray()));
	}

	public TupleValue deconstruct(Object arg) throws TokenException {
		if (arg instanceof TaggedValue) {
			TaggedValue v = (TaggedValue)arg;
			if (v.tag == this) {
				return new TupleValue(v.values);
			}
		}
		return null;
	}
	
	public String toString() {
		return tagName + "/" + arity;
	}
}
