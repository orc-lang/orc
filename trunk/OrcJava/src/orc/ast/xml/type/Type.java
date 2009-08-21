package orc.ast.xml.type;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.Config;
import orc.error.compiletime.typing.TypeException;

/**
 * @author quark
 */
@XmlSeeAlso(value={Blank.class, ArrowType.class, ClassnameType.class,
		Datatype.class, PolymorphicTypeAlias.class, SiteType.class,
		Top.class, TupleType.class, TypeApplication.class,
		TypeVariable.class})
public abstract class Type {
	/** Convert this syntactic type into an actual type.
	 * @return A new node.
	 * @throws TypeException 
	 */
	public abstract orc.type.Type unmarshal(Config config) throws TypeException;
}
