package orc.runtime.sites.core;


import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;
import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.TupleType;
import orc.type.Type;
import orc.type.TypeApplication;
import orc.type.TypeVariable;
import orc.type.ground.LetType;

/**
 * 
 * For each string argument, creates a datatype constructor site; the string is
 * used as a label for printing and debugging. Returns these sites as a tuple.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends EvalSite {

	@Override
	public Object evaluate(Args args) throws TokenException {
		
		Object[] datasites = new Object[args.size()];
		
		for(int i = 0; i < datasites.length; i++) {
			
			String label = args.stringArg(i);
			datasites[i] = new Datasite(label);
		}
		return Let.condense(datasites);
	}

	public Type type() {
		return new DatatypeSiteType();
	}
	
}

class DatatypeSiteType extends Type {
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		
		assert(typeActuals.size() == 1);
		orc.type.Datatype dt = (orc.type.Datatype)typeActuals.get(0); 
				
		/* Make sure each argument is a string */
		for (Arg a : args) {
			a.typecheck(Type.STRING, ctx, typectx);
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
			cResult = new TypeApplication(dt, params);
		}
				
		List<Type> cTypes = new LinkedList<Type>();
		for (List<Type> cArgs : dt.getConstructors()) {
			Type construct = new ArrowType(cArgs, cResult, cArity);
			Type destruct = new ArrowType(cResult, new TupleType(cArgs), cArity);
			
			DotType both = new DotType(construct);
			both.addField("?", destruct);
			cTypes.add(both);
		}
		
		return LetType.condense(cTypes);
	}
	
}