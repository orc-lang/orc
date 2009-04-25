package orc.ast.oil.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.tycon.DatatypeTycon;
import orc.type.tycon.Variance;

public class Datatype extends Type {
	@XmlAttribute(required=true)
	public String typename;
	@XmlElementWrapper(required=true)
	@XmlElement(name="member", required=true)
	public Type[][] members;
	@XmlAttribute(required=true)
	public int arity;
	
	public Datatype() {}
	public Datatype(String typename, Type[][] members, int arity) {
		this.typename = typename;
		this.members = members;
		this.arity = arity;
	}

	@Override
	public orc.type.Type unmarshal(Config config) throws TypeException {
		// We use this array to infer the variance of each type parameter
		Variance[] V = new Variance[arity];
		for (int i = 0; i < V.length; i++) {
			V[i] = Variance.CONSTANT;
		}
		
		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		List<List<orc.type.Type>> cs = new LinkedList<List<orc.type.Type>>();
		for (Type[] con : members) {
			List<orc.type.Type> ts = new LinkedList<orc.type.Type>();
			for (Type t : con) {
				// Convert the syntactic type to a true type
				orc.type.Type newT = t.unmarshal(config);
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
		
		return new DatatypeTycon(typename, vs, cs, new Object());
	}

}
