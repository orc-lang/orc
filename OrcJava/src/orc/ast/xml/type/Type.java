package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * @author quark, dkitchin
 */
@XmlSeeAlso(value={ArrowType.class, ClassnameType.class,
		Datatype.class, PolymorphicTypeAlias.class, SiteType.class,
		Top.class, TupleType.class, TypeApplication.class,
		TypeVariable.class})
public abstract class Type {
	/** Convert this syntactic type into an actual type.
	 * @return A new node.
	 */
	public abstract orc.ast.oil.type.Type unmarshal();
	
	public static List<orc.ast.oil.type.Type> unmarshalAll(Type[] ts) {
		List<orc.ast.oil.type.Type> newts = new LinkedList<orc.ast.oil.type.Type>();
		for (Type t : ts) {
			newts.add(t.unmarshal());
		}
		return newts;
	}
}
