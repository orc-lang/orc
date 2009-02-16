package orc.ast.simple.type;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

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
	
	
	/** Convert this syntactic type into an actual type, given an appropriate type context
	 * @param vars	The type environment, used in content addressable mode to 
	 * 				find the appropriate deBruijn index of a type variable.
	 * @return A new node.
	 * @throws TypeException 
	 */
	public abstract orc.type.Type convert(Env<String> env) throws TypeException;
	
}
