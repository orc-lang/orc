/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.reflect;

import java.util.Arrays;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.regions.LogicalClock;
import orc.runtime.regions.Region;
import orc.runtime.regions.SubRegion;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Closure;
import orc.type.Type;
import orc.type.structured.ArrowType;

/**
 * Implements the Site site.
 * 
 * This site promotes an Orc closure to a site, making it strict in its arguments,
 * requiring that it return only one value (subsequent returns are ignored), and
 * protecting it from termination.
 * 
 * @author dkitchin
 */
public class SiteSite extends PartialSite {
	public Type type() {
		return new SitemakerType();
	}

	public Object evaluate(Args args) throws TokenException {
		try {
			Closure f = (Closure)args.getArg(0);
			return new ClosureBackedSite(f);
		}
		catch (ClassCastException e) {
			// Argument must be a closure
			return null;
		}
	}
	
}

class SitemakerType extends Type {
	
	/**
	 * Ensure that there is exactly one argument, and it is an
	 * ArrowType, since all closures are of ArrowType.
	 * 
	 * The typechecker may still admit cases where sites which
	 * present type ArrowType are allowed to be arguments to
	 * this site. Such errors will be caught at runtime.
	 */
	public Type call(List<Type> args) throws TypeException {
		if (args.size() == 1) {
			Type T = args.get(0);
			if (T instanceof ArrowType) {
				return T;
			}
			else {
				throw new TypeException("Expected a closure; got a value of type " + T + " instead");
			}
		}
		else {
			throw new ArgumentArityException(1, args.size());
		}
	}
	
}

class ClosureBackedSite extends Site {

	Closure closure;
	
	public ClosureBackedSite(Closure closure) {
		this.closure = closure;
	}

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(orc.runtime.Args, orc.runtime.Token)
	 */
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		
		Token token = caller.getEngine().newExecution(new Silent(), // initial node is unused 
													  caller);
		
		token.setRegion(new ClosureExecRegion(caller, token.getRegion())); 
		  
		if (token != null) {
			List<Object> argsList = Arrays.asList(args.asArray());
			closure.createCall(token, argsList, new ClosureExecNode());
		}
		else {
			throw new SiteException("Failed to host closure execution as a site.");
		}
		
	}
	
}

class ClosureExecNode extends Node {

	/* (non-Javadoc)
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token)
	 */
	@Override
	public void process(Token t) {
		ClosureExecRegion cer = (ClosureExecRegion)t.getRegion();
		cer.onPublish(t.getResult());
		t.die();
	}
	
	public boolean isTerminal() {
		return true;
	}
	
}

class ClosureExecRegion extends SubRegion {
	
	Token caller = null;
	
	public ClosureExecRegion(Token caller, Region parent) {
		super(parent);
		this.caller = caller;
	}

	protected void onClose() {
		super.onClose();
		if (caller != null) {
			caller.die();
			caller = null;
		}
	}
	
	public void onPublish(Object v) {
		if (caller != null) {
			caller.resume(v);
			caller = null;
		}
	}
}