package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.expression.Expression;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * 
 * Abstract superclass of syntactic types in the simple AST.
 * 
 * Within the simple AST, bound type variables are represented by objects (TypeVariable),
 * rather than strings (NamedType). Free type variables may still occur (FreeTypeVariable),
 * but they must be eliminated before conversion to the next AST stage.
 * 
 * Syntactic types occur in all of the AST forms. The typechecker
 * converts them to a different form (subclasses of orc.type.Type)
 * for its own internal use.
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
	 * @param env	The type environment, used in content addressable mode to 
	 * 				find the appropriate deBruijn index of a type variable.
	 * @return A new node.
	 * @throws TypeException 
	 */
	public abstract orc.ast.oil.type.Type convert(Env<TypeVariable> env) throws TypeException;
	
	
	/**
	 * Convenience method, to apply convert to a list of types.
	 * 
	 * @param ts  A list of types
	 * @param env Environment for conversion
	 * @return The list of types, converted
	 * @throws TypeException
	 */
	public static List<orc.ast.oil.type.Type> convertAll(List<Type> ts, Env<TypeVariable> env) throws TypeException {
		
		if (ts != null) {
			List<orc.ast.oil.type.Type> newts = new LinkedList<orc.ast.oil.type.Type>();
		
			for (Type t : ts) {
				newts.add(t.convert(env));
			}
		
			return newts;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Performs the substitution [T/X], replacing occurrences of the free type variable X
	 * with the type T (which could be any type, including another variable).
	 * 
	 * @param T The replacing type
	 * @param X The free type variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the type with the substitution performed
	 */
	public abstract Type subst(Type T, FreeTypeVariable X);
	
	/**
	 * Convenience method, to apply a substitution to a list of types.
	 */
	public static List<Type> substAll(List<Type> ts, Type T, FreeTypeVariable X) {
		if (ts != null) {
			List<Type> newts = new LinkedList<Type>();
			for (Type t : ts) {
				Type newt = t.subst(T, X);
				newts.add(newt);
			}
			return newts;
		}
		else {
			return null;
		}
	}
	
	public static Type substMaybe(Type target, Type T, FreeTypeVariable X) {
		return (target != null ? target.subst(T, X) : null);
	}
	
	
	
	
	/**
	 * 
	 * Performs the substitution [U/X], replacing occurrences of the free type variable X
	 * with the nameless type variable U. Additionally, attach the name of X as documentation
	 * on U so that it can be used later for debugging or other purposes.
	 * 
	 * This method delegates to the subst method after attaching the name. 
	 * 
	 * @param U The replacing type variable
	 * @param X The free type variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the type with the substitution performed
	 */
	public Type subvar(TypeVariable U, FreeTypeVariable X) {
		U.name = X.name;
		return subst(U,X);
	}
	
	
	/**
	 * Perform a set of substitutions defined by a map.
	 * For each X |-> T in the map, the substitution [T/X] occurs.
	 * 
	 * If T is a nameless variable U, the name of the corresponding
	 * X will be attached to it.
	 * 
	 * @param m
	 */
	public Type subMap(Map<FreeTypeVariable, Type> m)
	{
		Type result = this;
		
		for (FreeTypeVariable x : m.keySet())
		{
			Type T = m.get(x);
			if (T instanceof TypeVariable) {
				result = result.subvar((TypeVariable)T, x);
			}
			else {
				result = result.subst(T,x);
			}
		}
		
		return result;
	}
	
}
