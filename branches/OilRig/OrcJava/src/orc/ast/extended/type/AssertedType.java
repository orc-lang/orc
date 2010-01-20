package orc.ast.extended.type;


/**
 * A type which is in some asserted position.
 * 
 * In later steps, assertion is represented by a flag on HasType; however, in the extended AST,
 * it is more useful to represent assertion as a type container, because it can be used in
 * many syntactic forms which have not yet been translated to HasType.
 * 
 * @author dkitchin
 *
 */
public class AssertedType extends Type {

	public Type type;
	
	public AssertedType(Type type) {
		this.type = type;
	}
	
	@Override
	public orc.ast.simple.type.Type simplify() {
		// asserted types should be gone by this step.
		throw new AssertionError("Unexpected: AssertedType");
	}
		
	public String toString() {		
		return type.toString() + " (asserted)";
	}	
	
}
