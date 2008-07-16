package orc.ast.simple.arg;


import orc.ast.oil.arg.Arg;
import orc.ast.val.Bool;
import orc.ast.val.Int;
import orc.ast.val.Str;
import orc.ast.val.Val;
import orc.env.Env;
import orc.runtime.values.Value;


/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Constant extends Argument {
	private static final long serialVersionUID = 1L;
	public Val v;
	
	public Constant(Integer i) {
		this.v = new Int(i);
	}
	
	public Constant(String s) {
		this.v = new Str(s);
	}
	
	public Constant(boolean b) {
		this.v = new Bool(b);
	}
	
	public Constant(Val v)
	{
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
