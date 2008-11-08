/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.ast.simple.arg.Var;
import orc.runtime.Token;
import orc.runtime.regions.SemiRegion;
import orc.runtime.values.Value;

/**
 * Compiled node for leaving the scope of a variable binding. 
 * @author dkitchin
 */
public class Leave extends Node {
	private static final long serialVersionUID = 1L;
	public Node next;

	public Leave(Node next) {
		this.next = next;
	}
	
	/** 
	 * When executed, relocate this token from its current
	 * region to that region's parent.
	 * The token moves to the next node and reactivates.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token)
	 */
	public void process(Token t) {
		// This cast cannot fail; a Leave node always matches a Semi node earlier in the dag.
		SemiRegion region = (SemiRegion)t.getRegion();
		
		// If a publication successfully leaves a SemiRegion, the right hand side of the semicolon shouldn't execute.
		// This step cancels the RHS.
		// It is an idempotent operation.
		region.cancel();
		
		t.setRegion(region.getParent()).move(next).activate();
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
