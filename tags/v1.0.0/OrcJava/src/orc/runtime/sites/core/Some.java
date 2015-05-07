/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.RuntimeTypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.TupleValue;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.ListType;
import orc.type.structured.OptionType;
import orc.type.structured.TupleType;

/**
 * Implements the "some" option constructor site.
 * 
 * @author quark
 */
public class Some extends Site {
	// since tags are compared by object equality,
	// we need to share a tag amongst all instances of this site
	static final Datasite data = new Datasite("Some");
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		data.callSite(args, caller);
	}
	
	public Type type() throws TypeException {
		Type X = new TypeVariable(0);
		Type OptionX = (new OptionType()).instance(X);
		
		Type construct = new ArrowType(X, OptionX, 1);
		
		List<Type> onlyX = new LinkedList<Type>();
		onlyX.add(X);
		Type destruct = new ArrowType(OptionX, new TupleType(onlyX), 1);
		
		DotType both = new DotType(construct);
		both.addField("?", destruct);
		
		return both;
	}

}
