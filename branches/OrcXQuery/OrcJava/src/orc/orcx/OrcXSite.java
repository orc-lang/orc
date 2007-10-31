package orc.orcx;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;


/**
 * 
 * Site object embedding an XQuery invocation.
 * 
 * Note that this is a slight deviation from how subclasses of Site typically behave:
 * an xquery invocation is strict in its arguments, but it may publish multiple values.
 * 
 * Semantically, this could be considered equivalent to returning a single sequence
 * value, and then invoking an 'explode' definition which traverses the sequence and
 * publishes each of its items.
 * 
 * @author dkitchin
 *
 */
public class OrcXSite extends Site {

	OrcX owner;
	int queryid;
	
	public OrcXSite(int queryid, OrcX owner) {
		this.queryid = queryid;
		this.owner = owner;
	}
	
	public void callSite(Args args, Token caller) {
		
		/* Call Galax, get a return value ret */
		caller.resume(owner.apply(queryid, args.getValues()));
	}

}
