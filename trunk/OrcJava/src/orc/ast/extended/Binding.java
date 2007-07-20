package orc.ast.extended;

public class Binding {
	public String var;
	public Expression exp;
		
	public Binding(String var, Expression exp)
	{
		this.var = var;
		this.exp = exp;	
	}	
	
}
