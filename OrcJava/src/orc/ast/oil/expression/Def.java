package orc.ast.oil.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.expression.argument.Variable;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.DefinitionArityException;
import orc.error.compiletime.typing.InsufficientTypeInformationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.error.compiletime.typing.UnspecifiedArgTypesException;
import orc.error.compiletime.typing.UnspecifiedReturnTypeException;

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
	public Expression body;
	public int typeArity;
	// FIXME: we need an OIL format for types
	public List<orc.ast.oil.type.Type> argTypes; /* May be null only if this def was derived from a lambda form, and only if it will be in a checking context. */
	public orc.ast.oil.type.Type resultType; /* May be null to request inference, which will succeed only for non-recursive functions */
	public SourceLocation location;
	
	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;
	
	public Def(int arity, Expression body, int typeArity, List<orc.ast.oil.type.Type> argTypes,
			orc.ast.oil.type.Type resultType, SourceLocation location, String name) {
		this.arity = arity;
		this.body = body;
		this.typeArity = typeArity;
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.location = location;
		this.name = name;
	}

	public final Set<Variable> freeVars() {
		Set<Integer> indices = new TreeSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Variable> vars = new TreeSet<Variable>();
		for (Integer i : indices) {
			vars.add(new Variable(i));
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
	public orc.type.structured.ArrowType type(Env<orc.type.Type> typectx) throws TypeException {
		
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
			return new orc.type.structured.ArrowType(orc.type.Type.substAll(orc.ast.oil.type.Type.transformAll(argTypes),typectx), resultType.transform().subst(typectx), typeArity);
		}
	}
	
	
	/* Make sure this definition checks against its stated type.
	 * 
	 * If the return type is missing, try to synthesize it from the type of the body.
	 * Any use of the function name will fail to typecheck if the return type is unspecified.
	 */
	public void checkDef(Env<orc.type.Type> ctx, Env<orc.type.Type> typectx) throws TypeException {
		
		Env<orc.type.Type> bodyctx = ctx;
		Env<orc.type.Type> newtypectx = typectx;
		
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
		for (orc.ast.oil.type.Type t : argTypes) {
			orc.type.Type actualT = t.transform().subst(newtypectx);
			bodyctx = bodyctx.extend(actualT);
		}

		/* Begin in checking mode if the result type is specified */
		if (resultType != null) {
			orc.type.Type actualResultType = resultType.transform().subst(newtypectx);
			body.typecheck(actualResultType, bodyctx, newtypectx);
		}
		/* Otherwise, synthesize it from the function body */
		else {
			// FIXME
			//resultType = body.typesynth(bodyctx, newtypectx);
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
	public void checkLambda(orc.type.structured.ArrowType t, Env<orc.type.Type> ctx, Env<orc.type.Type> typectx) throws TypeException {
		
		Env<orc.type.Type> bodyctx = ctx;
		Env<orc.type.Type> newtypectx = typectx;
		
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
				orc.type.Type U = t.argTypes.get(i);
				orc.type.Type V = argTypes.get(i).transform().subst(newtypectx);
				if (!U.subtype(V)) {
					throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
				}
			}
		}
		if (resultType != null) {
			if (!resultType.transform().subst(newtypectx).subtype(t.resultType)) {
				throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
			}
		}
		
		
		
		/* Add the argument types to the context */
		for (orc.type.Type ty : t.argTypes) {
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
			//FIXME
			//argTypes = t.argTypes;
		}
		if (resultType == null) {
			//FIXME
			//resultType = t.resultType;
		}
	}
	


	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
	public orc.ast.xml.expression.Def marshal() throws CompilationException {
		orc.ast.xml.type.Type newResultType = null;
		if (resultType != null) newResultType = resultType.marshal();
		orc.ast.xml.type.Type[] newArgTypes = null;
		if (argTypes != null) {
			newArgTypes = new orc.ast.xml.type.Type[argTypes.size()];
			int i = 0;
			for (orc.ast.oil.type.Type t : argTypes) {
				newArgTypes[i] = t.marshal();
				++i;
			}
		}
		return new orc.ast.xml.expression.Def(arity, body.marshal(),
				typeArity, newArgTypes, newResultType, location, name);
		
	}
}