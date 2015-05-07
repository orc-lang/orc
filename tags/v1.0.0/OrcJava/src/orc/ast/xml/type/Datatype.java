package orc.ast.xml.type;

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
	public String name;
	@XmlElementWrapper(required=true)
	@XmlElement(name="member", required=true)
	public Type[][] members;
	@XmlAttribute(required=true)
	public int arity;
	
	public Datatype() {}
	public Datatype(String name, Type[][] members, int arity) {
		this.name = name;
		this.members = members;
		this.arity = arity;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		
		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		List<List<orc.ast.oil.type.Type>> cs = new LinkedList<List<orc.ast.oil.type.Type>>();
		for (Type[] con : members) {
			List<orc.ast.oil.type.Type> ts = Type.unmarshalAll(con);
			cs.add(ts);
		}
		
		return new orc.ast.oil.type.Datatype(cs, arity, name);
	}

}
