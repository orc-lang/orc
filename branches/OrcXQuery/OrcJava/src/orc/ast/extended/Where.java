package orc.ast.extended;

public class Where extends Expression {

	public Expression left;
	public Expression right;
	public String var;
	
	public Where(Expression left, Expression right, String var)
	{
		this.left = left;
		this.right = right;
		this.var = var;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		orc.ast.simple.Expression newleft = left.simplify();
		orc.ast.simple.Expression newright = right.simplify();
		
		orc.ast.simple.arg.Var v = new orc.ast.simple.arg.Var();
		orc.ast.simple.arg.FreeVar x = new orc.ast.simple.arg.FreeVar(var);
		newleft.subst(v,x);
		newright.subst(v,x);
		
		return new orc.ast.simple.Where(newleft,newright,v);
	}

}
