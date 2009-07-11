package orc.ast.extended;

import java.util.List;

import orc.ast.extended.pattern.Pattern;

public class CatchHandler{
	
	public List<Pattern> catchPattern;
	public Expression body;
	
	public CatchHandler(List<Pattern> formals, Expression body){
		this.catchPattern = formals;
		this.body = body;
	}
	
	public String toString(){
		return "catch(" + catchPattern.toString() + ")" + body.toString();
	}
}
