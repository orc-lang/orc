/*
 /*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.*;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;

/**
 * Compiled node for an eval of either a definition with no formals
 * or a variable.
 * @author mbickford
 */
public class Eval extends Node {
	private static final long serialVersionUID = 1L;
	String name;
	Node next;

	public Eval(String name, Node next) {
		this.name = name;
		this.next = next;
	}

	/** 
	 * For v == name, we have to:
	 * 1) if v is unbound we create a call to "let(x) where x in v" so that we wait on v.
	 * 2) if v is bound to a closure ?? 
	 * or to "v()" depending on ??
	 */
	public void process(Token t, OrcEngine engine) {
		Value holder = t.lookup(name);
		List<Param> params = new ArrayList<Param>();
		if (holder.asUnboundCell() == null){
			if (holder.Callable0()) {
				Callable target = (Callable) holder;
				target.createCall(name, t, params, next, engine);
			}
			else  {
				t.setResult(holder.asBasicValue());
				t.move(next);
				engine.activate(t);
			}
		}
		else {
			String var = new String("temp");
		    params.add(new Variable(var));
			Node base = new Call("let", params, next,null); 
			List<Param> params2 = new ArrayList<Param>();
			params2.add(new Variable(name));
			Node item = new Call("let", params2, new Store(var),null);
			new Where(base, var, item).process(t,engine);
			}
			
		
	}
}
