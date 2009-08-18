package orc.ast.extended.declaration.defn;

import java.util.Map;

import orc.ast.extended.ASTNode;
import orc.error.Locatable;
import orc.error.SourceLocation;

public abstract class Defn implements ASTNode, Locatable {

	public String name;
	private SourceLocation location;
	public abstract void extend(AggregateDefn adef);
	
	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
}
