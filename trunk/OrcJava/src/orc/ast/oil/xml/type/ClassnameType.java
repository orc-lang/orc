package orc.ast.oil.xml.type;

import javax.xml.bind.annotation.XmlAttribute;

import orc.Config;

/**
 * A syntactic type which refers to a Java class (which we will treat as a type).
 * @author quark, dkitchin
 */
public class ClassnameType extends Type {
	@XmlAttribute(required=true)
	public String classname;
	
	public ClassnameType() {}
	public ClassnameType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.type.Type unmarshal(Config config) {
		// FIXME: generate a more useful type
		return new orc.type.ClassnameType(classname);
	}
		
	public String toString() {		
		return classname;
	}	
}
