package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;


/**
 * 
 * Abstract superclass of all syntactic types.
 * 
 * Syntactic types occur in extended and simple ASTs. They must be converted
 * to actual types (orc.type.*) before use in the Orc typechecker.
 * 
 * Syntactic types do not have methods like meet, join, and subtype; their
 * actual type counterparts do. Thus, syntactic types permit only the simplest
 * analyses; more complex analysis must wait until the syntactic type is
 * resolved to an actual type.
 * 
 * All syntactic types can be written explicitly in a program, whereas
 * some actual types are only generated during typechecking itself.
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
	public abstract orc.type.Type transform();
	
	/**
	 * Convert this type into an XML-isomorphic format for serialization.
	 */
	public abstract orc.ast.xml.type.Type marshal();

	/**
	 * Convenience method, to marshal a list of types.
	 */
	public static orc.ast.xml.type.Type[] marshalAll(List<Type> ts) {
		
		orc.ast.xml.type.Type[] newts = new orc.ast.xml.type.Type[ts.size()];
		int i = 0;
		for (Type t : ts) {
			newts[i++] = t.marshal();
		}
		
		return newts;
	}
	
	
	/**
	 * Convenience method, to transform a list of types.
	 * 
	 * @param ts  A list of types
	 * @param env Environment for conversion
	 * @return The list of types, converted
	 * @throws TypeException
	 */
	public static List<orc.type.Type> transformAll(List<Type> ts) {
		
		List<orc.type.Type> newts = new LinkedList<orc.type.Type>();
		
		for (Type t : ts) {
			newts.add(t.transform());
		}
		
		return newts;
	}
	
}
