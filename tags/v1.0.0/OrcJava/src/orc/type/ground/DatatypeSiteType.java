package orc.type.ground;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.sites.core.Let;
import orc.type.Type;
import orc.type.TypeApplication;
import orc.type.TypeVariable;
import orc.type.TypingContext;
import orc.type.ground.LetType;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.TupleType;
import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;

public class DatatypeSiteType extends Type {
	
	public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		
		assert(typeActuals.size() == 1);
		orc.type.tycon.DatatypeTycon dt = (orc.type.tycon.DatatypeTycon)typeActuals.get(0); 
				
		/* Make sure each argument is a string */
		for (Argument a : args) {
			a.typecheck(ctx, Type.STRING);
		}
		
		/* Find the type arity for each constructor */
		int cArity = dt.variances().size();
		
		/* Manufacture the result type as an instance of
		 * the datatype. If it has parameters, apply
		 * it to the bound parameters.
		 */
		Type cResult = dt;
		if (cArity > 0) {
			List<Type> params = new LinkedList<Type>();
			for(int i = cArity - 1; i >= 0; i--) {
				params.add(new TypeVariable(i));
			}
			cResult = dt.instance(params);
		}
				
		List<Type> cTypes = new LinkedList<Type>();
		for (List<Type> cArgs : dt.getConstructors()) {
			Type construct = new ArrowType(cArgs, cResult, cArity);
			Type destruct = new ArrowType(cResult, LetType.condense(cArgs), cArity);
			
			DotType both = new DotType(construct);
			both.addField("?", destruct);
			cTypes.add(both);
		}
		
		return LetType.condense(cTypes);
	}
	
}