package orc.ast.extended.type;


/**
 * 
 * Abstract superclass of all syntactic types.
 * 
 * Syntactic types occur in extended and simple ASTs. They must be converted
 * to actual types (orc.type.*) before use in the Orc typechecker, since they
 * use a named representation of type variables. This conversion occurs when
 * the simple AST is translated to OIL.
 * 
 * Syntactic types do not have methods like meet, join, and subtype; their
 * actual type counterparts do. Thus, syntactic types permit only the simplest
 * analyses; more complex analysis must wait until the syntactic type is
 * resolved to an actual type.
 * 
 * All syntactic types can be written explicitly in a program, whereas
 * some actual types are only generated during compiler translations or during
 * typechecking itself.
 * 
 * @author dkitchin
 *
 */
public abstract class Type {

	/* Create singleton representatives for some common types */
	public static final Type BLANK = new Blank();
	public static final Type TOP = new Top();
	public static final Type BOT = new Bot();
	
	/** Convert this syntactic type into an actual type, given an appropriate type context
	 * @return A new node.
	 */
	public abstract orc.ast.simple.type.Type simplify();
	
}
