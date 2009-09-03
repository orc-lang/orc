package orc.ast.xml.type;

import javax.xml.bind.annotation.XmlAttribute;


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
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.ClassType(classname);
	}
		
	public String toString() {		
		return classname;
	}	
}
