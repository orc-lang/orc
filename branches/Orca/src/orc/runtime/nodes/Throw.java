package orc.runtime.nodes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.runtime.Token;
import orc.runtime.values.Future;
import orc.runtime.values.Value;
import orc.runtime.values.Callable;
import orc.runtime.values.Closure;

import orc.error.runtime.TokenLimitReachedError;
import orc.error.runtime.UncallableValueException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncaughtException;

/**
 * @author matsuoka 
 */

import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Variable;
import orc.runtime.values.Callable;

public class Throw extends Node {
	
	public Throw(){}
	
	public void process(Token t) {
		
		Object o = t.getResult();
			
		try {
			t.throwException(o);
		}
		catch (TokenException e) {
			t.error(e);
		}
	}
}
