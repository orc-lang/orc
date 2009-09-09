package orc.ast.xml.type;

import javax.xml.bind.annotation.XmlAttribute;


/**
 * A simple named type.
 * 
 * @author dkitchin
 *
 */
public class TypeVariable extends Type {
	@XmlAttribute(required=true)
	public int index;
	@XmlAttribute(required=false)
	public String name;
	
	public TypeVariable(int index) {
		this(index, null);
	}
	public TypeVariable(int index, String name) {
		this.index = index;
		this.name = name;
	}
	
	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.TypeVariable(index, name);
	}
}
