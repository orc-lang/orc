package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Variance;

/**
 * 
 * A syntactic type encompassing all of the information associated with a datatype.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends Type {

	public String typename;
	public List<Constructor> members;
	public List<VariantTypeFormal> formals;
	
	public Datatype(String typename, List<Constructor> members, List<VariantTypeFormal> formals) {
		this.typename = typename;
		this.members = members;
		this.formals = formals;
	}

	@Override
	public orc.type.Type convert(Env<String> env) throws TypeException {
		
		List<Variance> vs = new LinkedList<Variance>();
		for (VariantTypeFormal formal : formals) {
			vs.add(formal.variance);
		}
		
		// First, add the datatype name itself to the context
		env = env.extend(typename);
		
		// Then, add the type parameters
		for (VariantTypeFormal formal : formals) {
			env = env.extend(formal.name);
		}
		
		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		List<List<orc.type.Type>> cs = new LinkedList<List<orc.type.Type>>();
		for (Constructor con : members) {
			List<orc.type.Type> ts = new LinkedList<orc.type.Type>();
			for (Type t : con.args) {
				ts.add(t.convert(env));
			}
			cs.add(ts);
		}
		
		return new orc.type.Datatype(typename, vs, cs);
	}

}
