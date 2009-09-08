package orc.ast.oil.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.type.InferredType;
import orc.ast.oil.type.Type;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.DefinitionArityException;
import orc.error.compiletime.typing.InsufficientTypeInformationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.error.compiletime.typing.UnspecifiedArgTypesException;
import orc.error.compiletime.typing.UnspecifiedReturnTypeException;
import orc.type.TypingContext;

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
	public orc.type.structured.ArrowType type(TypingContext ctx) throws TypeException {
		
		for(int i = 0; i < typeArity; i++) {
			ctx = ctx.bindType(null);
		}
		
		if (argTypes == null) {
			throw new UnspecifiedArgTypesException();
		}
		else if (resultType == null) {
			throw new UnspecifiedReturnTypeException();
		}
		else {
			return new orc.type.structured.ArrowType(ctx.promoteAll(argTypes), ctx.promote(resultType), typeArity);
		}
	}
	
	
	/* Make sure this definition checks against its stated type.
	 * 
	 * If the return type is missing, try to synthesize it from the type of the body.
	 * Any use of the function name will fail to typecheck if the return type is unspecified.
	 */
	public void checkDef(TypingContext ctx) throws TypeException {
		
		if (argTypes == null) { 
			throw new UnspecifiedArgTypesException(); 
		}
		
		/* Add the type arguments to the type context */
		for(int i = 0; i < typeArity; i++) {
			ctx = ctx.bindType(null);
		}
		
		/* Make sure the function arity corresponds to the number of argument types */
		if (argTypes.size() != arity) {
			throw new DefinitionArityException(argTypes.size(), arity);
		}
		
		/* Add the argument types to the context */
		for (orc.ast.oil.type.Type t : argTypes) {
			orc.type.Type actualT = ctx.promote(t);
			ctx = ctx.bindVar(actualT);
		}

		/* Begin in checking mode if the result type is specified */
		if (resultType != null) {
			orc.type.Type actualResultType = ctx.promote(resultType);
			body.typecheck(ctx, actualResultType);
		}
		/* Otherwise, synthesize it from the function body */
		else {
			resultType = new InferredType(body.typesynth(ctx));
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
	public void checkLambda(TypingContext ctx, orc.type.structured.ArrowType t) throws TypeException {
		
		/* Add the type arguments to the type context */
		for(int i = 0; i < typeArity; i++) {
			ctx = ctx.bindType(null);
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
				orc.type.Type V = ctx.promote(argTypes.get(i));
				if (!U.subtype(V)) {
					throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
				}
			}
		}
		if (resultType != null) {
			orc.type.Type U = t.resultType;
			orc.type.Type V = ctx.promote(resultType);
			if (!V.subtype(U)) {
				throw new TypeException("Checked type " + t + " did not match type ascriptions on this lambda.", getSourceLocation());
			}
		}
		
		
		
		/* Add the argument types to the context */
		for (orc.type.Type ty : t.argTypes) {
			ctx = ctx.bindVar(ty);
		}
		
		/* Check the body against the return type */
		body.typecheck(ctx, t.resultType);
		
		
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
			argTypes = orc.ast.oil.type.Type.inferredTypes(t.argTypes);
		}
		if (resultType == null) {
			resultType = new InferredType(t.resultType);
		}
	}
	


	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
	public orc.ast.xml.expression.Def marshal() throws CompilationException {
		orc.ast.xml.type.Type newResultType = (resultType != null ? resultType.marshal() : null);
		orc.ast.xml.type.Type[] newArgTypes = Type.marshalAll(argTypes);
		return new orc.ast.xml.expression.Def(arity, body.marshal(),
				typeArity, newArgTypes, newResultType, location, name);
		
	}
}