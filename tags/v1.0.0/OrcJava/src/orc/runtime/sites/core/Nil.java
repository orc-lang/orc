/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites.core;

import orc.error.compiletime.typing.TypeException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.NilValue;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.ListType;

/**
 * Implements the empty list constructor.
 * 
 * @author dkitchin
 */
public class Nil extends EvalSite {

	
	@Override
	public Object evaluate(Args args) {
		return NilValue.singleton;
	}

	public Type type() throws TypeException {
		return new ArrowType((new ListType()).instance(Type.BOT)); 
	}
	
}
