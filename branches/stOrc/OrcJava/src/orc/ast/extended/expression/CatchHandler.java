package orc.ast.extended.expression;

import java.util.List;

import orc.ast.extended.ASTNode;
import orc.ast.extended.Visitor;
import orc.ast.extended.pattern.Pattern;
import orc.error.SourceLocation;

public class CatchHandler implements ASTNode {
	private SourceLocation location;
	
	public List<Pattern> catchPattern;
	public Expression body;
	
	public CatchHandler(List<Pattern> formals, Expression body){
		this.catchPattern = formals;
		this.body = body;
	}

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

	public String toString(){
		return "catch(" + catchPattern.toString() + ")" + body.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
