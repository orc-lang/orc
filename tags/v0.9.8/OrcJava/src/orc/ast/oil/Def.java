package orc.ast.oil;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.typing.DefinitionArityException;
import orc.error.compiletime.typing.InsufficientTypeInformationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnspecifiedArgTypesException;
import orc.error.compiletime.typing.UnspecifiedReturnTypeException;
import orc.type.ArrowType;
import orc.type.Type;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Groups of mutually recursive definitions are scoped in the simplified abstract syntax tree by a Def.
 * 
 * @author dkitchin
 *
 */

public class Def implements Locatable {

	public int arity;
	public Expr body;
	public int typeArity;
	// FIXME: we need an OIL format for types
	public List<Type> argTypes; /* May be null only if this def was derived from a lambda form, and only if it will be in a checking context. */
	public Type resultType; /* May be null to request inference, which will succeed only for non-recursive functions */
	public SourceLocation location;
	
	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;
	
	public Def(int arity, Expr body, int typeArity, List<Type> argTypes,
			Type resultType, SourceLocation location, String name) {
		this.arity = arity;
		this.body = body;
		this.typeArity = typeArity;
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.location = location;
		this.name = name;
	}

	public final Set<Var> freeVars() {
		Set<Integer> indices = new TreeSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Var> vars = new TreeSet<Var>();
		for (Integer i : indices) {
			vars.add(new Var(i));
		}
		
		return vars;
	}
	
	public void addIndices(Set<Integer> indices, int depth) {
		body.addIndices(indices, depth + arity);
	}
	
	public String toString() {
		
		String args = "";
		for(int i = 0; i < arity; i++) 
			args += "."; 
		return "(def " + args + " " + body.toString() + ")";
	}
	
	/* Construct an arrow type from the type information contained in this definition 
	 * This construction fails if the result type or arg types are null.
	 */
	public ArrowType type(Env<Type> typectx) throws TypeException {
		
		for(int i = 0; i < typeArity; i++) {
			typectx = typectx.extend(null);
		}
		
		if (argTypes == null) {
			throw new UnspecifiedArgTypesException();
		}
		else if (resultType == null) {
			throw new UnspecifiedReturnTypeException();
		}
		else {
			return new ArrowType(Type.substAll(argTypes,typectx), resultType.subst(typectx), typeArity);
		}
	}
	
	
	/* Make sure this definition checks against its stated type.
	 * 
	 * If the return type is missing, try to synthesize it from the type of the body.
	 * Any use of the function name will fail to typecheck if the return type is unspecified.
	 */
	public void checkDef(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		Env<Type> bodyctx = ctx;
		Env<Type> newtypectx = typectx;
		
		if (argTypes == null) { 
			throw new UnspecifiedArgTypesException(); 
		}
		
		/* Add the type arguments to the type context */
		for(int i = 0; i < typeArity; i++) {
			newtypectx = newtypectx.extend(null);
		}
		
		/* Make sure the function arity corresponds to the number of argument types */
		if (argTypes.size() != arity) {
			throw new DefinitionArityException(argTypes.size(), arity);
		}
		
		/* Add the argument types to the context */
		for (Type t : argTypes) {
			Type actualT = t.subst(newtypectx);
			bodyctx = bodyctx.extend(actualT);
		}

		/* Begin in checking mode if the result type is specified */
		if (resultType != null) {
			Type actualResultType = resultType.subst(newtypectx);
			body.typecheck(actualResultType, bodyctx, newtypectx);
		}
		/* Otherwise, synthesize it from the function body */
		else {
			resultType = body.typesynth(bodyctx, newtypectx);
		}
	}
	
	
	/* Check this definition using the given arrow type.
	 * If checking succeeds, replace any missing type components of
	 * this definition with the information from the arrow type.
	 * 
	 * This is used to infer the argument and result types of
	 * unannotated lambda functions when they occur in a checking
	 * context that provides that information.
	 */
	public void checkLambda(ArrowType t, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		Env<Type> bodyctx = ctx;
		Env<Type> newtypectx = typectx;
		
		/* Add the type arguments to the type context */
		for(int i = 0; i < typeArity; i++) {
			newtypectx = typectx.extend(null);
		}
		
		/* Make sure that if the definition does have type information,
		 * that type information is compatible with the arrow type being
		 * checked against. 
		 */
		if (t.typeArity != typeArity) {
			throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
		}
		
		if (argTypes != null) {
			for (int i = 0; i < t.argTypes.size(); i++) {
				Type U = t.argTypes.get(i);
				Type V = argTypes.get(i).subst(newtypectx);
				if (!U.subtype(V)) {
					throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
				}
			}
		}
		if (resultType != null) {
			if (!resultType.subst(newtypectx).subtype(t.resultType)) {
				throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
			}
		}
		
		
		
		/* Add the argument types to the context */
		for (Type ty : t.argTypes) {
			bodyctx = bodyctx.extend(ty);
		}
		
		/* Check the body against the return type */
		body.typecheck(t.resultType, bodyctx, newtypectx);
		
		
		/* Fill in any missing type information on this def.
		 * 
		 * Note that the components of t are instantiated types; they will never
		 * contain free type variables, so the checker does not attempt to infer
		 * occurrences of type variables (as it should not).
		 * 
		 * For example,
		 * 
		 * (lambda(x) = x) :: (lambda(Integer) :: Integer)
		 * 
		 * will fill in the particular annotations
		 * 
		 * lambda(x :: Integer) :: Integer = x
		 * 
		 * but it cannot infer the more general polymorphic type
		 * 
		 * lambda[A](x :: A) :: A = x
		 * 
		 */
		if (argTypes == null) {
			argTypes = t.argTypes;
		}
		if (resultType == null) {
			resultType = t.resultType;
		}
	}
	


	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
}