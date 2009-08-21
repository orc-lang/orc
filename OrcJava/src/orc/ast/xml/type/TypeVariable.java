package orc.ast.xml.type;

import javax.xml.bind.annotation.XmlAttribute;

import orc.Config;
import orc.error.compiletime.typing.TypeException;

/**
 * A simple named type.
 * 
 * @author dkitchin
 *
 */
public class TypeVariable extends Type {
	@XmlAttribute(required=true)
	public int index;
	
	public TypeVariable() {}
	public TypeVariable(int index) {
		this.index = index;
	}
	
	@Override
	public orc.type.Type unmarshal(Config config) throws TypeException {
		return new orc.type.TypeVariable(index);
	}
}
