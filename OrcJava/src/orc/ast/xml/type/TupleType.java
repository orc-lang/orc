package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;


/**
 * A syntactic type tuple: (T,...,T)
 * 
 * @author quark, dkitchin
 */
public class TupleType extends Type {
	@XmlElement(name="item", required=true)
	public Type[] items;
	
	public TupleType() {}
	public TupleType(Type[] items) {
		this.items = items;
	}
	
	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.TupleType(Type.unmarshalAll(items));
	}
}
