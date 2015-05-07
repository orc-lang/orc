package orc.ast.extended.declaration.def;

import java.util.Map;

import orc.ast.extended.ASTNode;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public abstract class DefMember implements ASTNode, Locatable {

	public String name;
	private SourceLocation location;
	public abstract void extend(AggregateDef adef) throws CompilationException;
	
	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
}
