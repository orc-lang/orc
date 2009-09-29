package orc.runtime.nodes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Variable;
import orc.env.Env;
import orc.runtime.Token;
import orc.runtime.values.Closure;
import orc.runtime.values.Future;

public class PushHandler extends Node {
	
	Def handler;
	Node tryBlock;
	Node next;
	
	public PushHandler(Def handler, Node tryBlock, Node next){
		this.handler = handler;
		this.tryBlock = tryBlock;
		this.next = next;
	}
	
	public void process(Token t){
		
		/* 
		 * Create a closure to do the call.
		 */
		Set<Variable> free = handler.free;
		List<Object> freeValues = new LinkedList<Object>();
		for (Variable v : free) {
			Object value = v.resolve(t.getEnvironment());
			if (value instanceof Future) freeValues.add(value);
		}
		
		Closure closure = new Closure(handler, freeValues);
		Env<Object> env = new Env<Object>();
		for (Variable v : handler.free) env.add(v.resolve(t.getEnvironment()));
		closure.env = env;
		
		//	pass next so the handler knows where to return.
		t.pushHandler(closure, next);
		t.move(tryBlock).activate();
	}

}
