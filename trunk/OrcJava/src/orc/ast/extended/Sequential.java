package orc.ast.extended;

public class Sequential extends Expression {

	public Expression left;
	public Expression right;
	public String var;
	public boolean pub;
	
	public Sequential(Expression left, Expression right, String var, Boolean pub)
	{
		this.left = left;
		this.right = right;
		this.var = var;
		this.pub = pub;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		orc.ast.simple.Expression newleft = left.simplify();
		orc.ast.simple.Expression newright = right.simplify();
		
		orc.ast.simple.arg.Var v = new orc.ast.simple.arg.Var();
		orc.ast.simple.arg.FreeVar x = new orc.ast.simple.arg.FreeVar(var);
		newleft.subst(v,x);
		newright.subst(v,x);
		
		if (pub) 
		{
			newright = new orc.ast.simple.Parallel(new orc.ast.simple.Let(v), newright);
		}
		
		return new orc.ast.simple.Sequential(newleft,newright,v);
	}

}
