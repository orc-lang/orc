package orc.orchard.values;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.ast.xml.expression.argument.Constant;
import orc.ast.xml.expression.argument.Field;
import orc.ast.xml.expression.argument.Site;

/**
 * Orc publishable values.
 * 
 * @author quark
 */
@XmlSeeAlso(value={Constant.class, Field.class, Site.class, UnrepresentableValue.class, List.class, Tuple.class, Tagged.class})
public abstract class Value {}
