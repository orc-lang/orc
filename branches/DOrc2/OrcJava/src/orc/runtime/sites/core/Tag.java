package orc.runtime.sites.core;


import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.TaggedValue;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

// Site for generating individual constructor sites for datatypes. Deprecated.

public class Tag extends EvalSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		return new Datasite(args.stringArg(1));
	}

}
