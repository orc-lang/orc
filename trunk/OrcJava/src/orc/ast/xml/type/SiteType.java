package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeVariable;

/**
 * A syntactic type corresponding to a Java class implementing a type.
 * 
 * In order to convert this to an actual type, the Java class must be
 * a subtype of orc.type.Type
 * 
 * @author dkitchin
 *
 */
public class SiteType extends Type {
	@XmlAttribute(required=true)
	public String classname;
	
	public SiteType() {}
	
	public SiteType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.SiteType(classname);
	}
}
