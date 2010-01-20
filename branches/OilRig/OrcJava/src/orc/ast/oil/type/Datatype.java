package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;
import orc.type.tycon.DatatypeTycon;
import orc.type.tycon.Variance;

/**
 * 
 * A type encompassing all of the information associated with a datatype.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends Type {

	public List<List<Type>> members;
	public int typeArity;
	String name;
	
	public Datatype(List<List<Type>> members, int typeArity, String name) {
		this.members = members;
		this.typeArity = typeArity;
		this.name = name;
	}

	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		
		// We use this array to infer the variance of each type parameter
		Variance[] V = new Variance[typeArity];
		for (int i = 0; i < V.length; i++) {
			V[i] = Variance.CONSTANT;
		}
		
		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		List<List<orc.type.Type>> cs = new LinkedList<List<orc.type.Type>>();
		for (List<Type> con : members) {
			List<orc.type.Type> ts = new LinkedList<orc.type.Type>();
			for (Type t : con) {
				// Convert the syntactic type to a true type
				orc.type.Type newT = t.transform(ctx);
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
		
		return new DatatypeTycon(name, vs, cs, this);
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		
		orc.ast.xml.type.Type[][] cs = new orc.ast.xml.type.Type[members.size()][0];
		int i = 0;
		for (List<Type> ts : members) {
			cs[i++] = Type.marshalAll(ts);
		}
		
		return new orc.ast.xml.type.Datatype(name, cs, typeArity);
	}

}
