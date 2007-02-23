/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;

/**
 * A value container for a literal value
 * @author wcook
 */
public class Constant extends BaseValue  implements Callable {
	private static final long serialVersionUID = 1L;
	Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public Object asBasicValue() {
		return value;
	}
	public String toString() {
		return value.toString();
	}
	/**
	 * We can "call" a constant if its value is callable.
	 * --Mark Bickford
	**/ 
	public void createCall(String label, Token callToken,
			List<Param> args, Node nextNode, OrcEngine engine) {
		if (engine.debugMode){
			engine.debug("Call constant " + toString(),
					callToken);
			}

		if (value instanceof Callable){
			Callable target = (Callable) value;
			target.createCall(label,callToken,args,nextNode,engine);
		}
		else {
			if (engine.debugMode){
				engine.debug("Constant does not have a callable value!",callToken );
				}
		}
	}
	
	

}
