package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;


/**
 * 
 * Abstract superclass of syntactic types in the OIL AST.
 * 
 * Within the OIL AST, bound type variables are represented by deBruijn
 * indices, a serializable name-free representation. 
 * 
 * Syntactic types occur in all of the AST forms. The typechecker
 * converts them to a different form (subclasses of orc.type.Type)
 * for its own internal use.
 * 
 * The promote method converts a syntactic OIL type to a form that the
 * typechecker can use. Types from within the typechecker may be
 * embedded within OIL using an InferredType; however, these are
 * temporary placeholders only, and are erased by XML serialization.
 * 
 * Syntactic types do not have methods like meet, join, and subtype; only their
 * typechecker counterparts do. Thus, syntactic types permit only the simplest
 * analyses; more complex analyses must wait until the syntactic type is
 * converted within the typechecker.
 * 
 * All syntactic types can be written explicitly in a program, whereas
 * many of the typechecker's internal types are not representable in programs.
 * 
 * @author dkitchin
 *
 */
public abstract class Type {

	/* Create singleton representatives for some common types */
	public static final Type TOP = new Top();
	public static final Type BOT = new Bot();
	
	/** Convert this syntactic type into an actual type, given an appropriate type context
	 * @param ctx TODO
	 * @return A new node.
	 * @throws TypeException TODO
	 */
	public abstract orc.type.Type transform(TypingContext ctx) throws TypeException;
	
	/**
	 * Convert this type into an XML-isomorphic format for serialization.
	 */
	public abstract orc.ast.xml.type.Type marshal();

	/**
	 * Convenience method, to marshal a list of types.
	 */
	public static orc.ast.xml.type.Type[] marshalAll(List<Type> ts) {
		
		if (ts == null) { return null; }
		orc.ast.xml.type.Type[] newts = new orc.ast.xml.type.Type[ts.size()];
		int i = 0;
		for (Type t : ts) {
			/* If any type is inferred, the whole list is unmarshalable */
			if (t instanceof InferredType) {
				return null;
			}
			newts[i++] = t.marshal();
		}
		
		return newts;
	}
	
	
	/**
	 * Convenience method, to transform a list of types.
	 * 
	 * @param ts  A list of types
	 * @param ctx TODO
	 * @param env Environment for conversion
	 * @return The list of types, converted
	 * @throws TypeException 
	 * @throws TypeException
	 */
	public static List<orc.type.Type> transformAll(List<Type> ts, TypingContext ctx) throws TypeException {
		
		List<orc.type.Type> newts = new LinkedList<orc.type.Type>();
		
		for (Type t : ts) {
			newts.add(t.transform(ctx));
		}
		
		return newts;
	}

	/**
	 * @param argTypes
	 * @return
	 */
	public static List<orc.ast.oil.type.Type> inferredTypes(List<orc.type.Type> argTypes) {
		
		List<orc.ast.oil.type.Type> inferred = new LinkedList<orc.ast.oil.type.Type>();
		for(orc.type.Type t : argTypes) {
			inferred.add(new InferredType(t));
		}
		return inferred;
	}
	
}
