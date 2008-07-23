package orc.ast.simple.arg;


import orc.ast.oil.arg.Arg;
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
		return super.toString() + "(" + v.toString() + ")";
	}

	@Override
	public Arg convert(Env<Var> vars) {	
		return new orc.ast.oil.arg.Constant(v);
	}
}
