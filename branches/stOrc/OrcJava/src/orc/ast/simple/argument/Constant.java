package orc.ast.simple.argument;


import orc.env.Env;


/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Constant extends Argument {
	private static final long serialVersionUID = 1L;
	public Object v;
	
	public Constant(Object v) {
		this.v = v;
	}
	
	public String toString() {
		return String.valueOf(v);
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) {	
		return new orc.ast.oil.expression.argument.Constant(v);
	}
}
