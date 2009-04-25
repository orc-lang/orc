package orc.ast.oil.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.error.compiletime.typing.TypeException;

/**
 * A syntactic type tuple: (T,...,T)
 * 
 * @author quark
 */
public class TupleType extends Type {
	@XmlElement(name="item", required=true)
	public Type[] items;
	
	public TupleType() {}
	public TupleType(Type[] items) {
		this.items = items;
	}
	
	@Override
	public orc.type.Type unmarshal(Config config) throws TypeException {
		LinkedList<orc.type.Type> newitems = new LinkedList<orc.type.Type>();
		
		for (Type T : items) {
			newitems.add(T.unmarshal(config));
		}
		
		return new orc.type.TupleType(newitems);
	}
}
