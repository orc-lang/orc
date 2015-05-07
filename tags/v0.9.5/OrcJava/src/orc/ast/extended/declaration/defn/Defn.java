package orc.ast.extended.declaration.defn;

import java.util.Map;

public abstract class Defn {

	public String name;
	public abstract void extend(AggregateDefn adef);
	
}
