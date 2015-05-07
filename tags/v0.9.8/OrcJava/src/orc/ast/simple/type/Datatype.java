package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.tycon.DatatypeTycon;
import orc.type.tycon.Variance;

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
	public List<String> formals;
	Object id;
	
	public Datatype(String typename, List<Constructor> members, List<String> formals) {
		this.typename = typename;
		this.members = members;
		this.formals = formals;
		this.id = new Object(); // identifier for this syntactic occurrence
	}

	@Override
	public orc.type.Type convert(Env<String> env) throws TypeException {
		
		// First, add the datatype name itself to the context
		env = env.extend(typename);
		
		// Then, add the type parameters
		for (String formal : formals) {
			env = env.extend(formal);
		}
		
		// We use this array to infer the variance of each type parameter
		Variance[] V = new Variance[formals.size()];
		for (int i = 0; i < V.length; i++) {
			V[i] = Variance.CONSTANT;
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
				// Convert the syntactic type to a true type
				orc.type.Type newT = t.convert(env);
				// Add it as an entry for the new constructor
				ts.add(newT);
				// Infer the variance of each type parameter it uses;
				// add that information to the array V.
				for (int i = 0; i < V.length; i++) {
					V[i] = V[i].and(newT.findVariance(i));
				}
			}
			cs.add(ts);
		}

		List<Variance> vs = new LinkedList<Variance>();
		for(Variance v : V) {
			vs.add(0,v);
		}
		
		return new DatatypeTycon(typename, vs, cs, id);
	}

}
