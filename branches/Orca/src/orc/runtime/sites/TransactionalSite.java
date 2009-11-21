/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.sites;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.NontransactionalSiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;
import orc.runtime.values.Visitor;
import orc.type.Type;

/**
 * Base class for transactional sites.
 * 
 * These sites set a different version of the callSite method as abstract, to be overridden.
 * 
 * 
 * @author wcook, dkitchin
 */
public abstract class TransactionalSite extends Site implements Callable {

	/**
	 * Must be implemented by subclasses to implement the site behavior
	 */
	abstract public void callSite(Args args, Token caller, Transaction transaction) throws TokenException;
	
	@Override
	public void callSite(Args args, Token t) throws TokenException {
		callSite(args, t, null);
	}
	
}
