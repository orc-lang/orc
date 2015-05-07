package orc.orchard.values;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.ast.oil.xml.Constant;
import orc.ast.oil.xml.Field;
import orc.ast.oil.xml.Site;

/**
 * Orc publishable values.
 * 
 * @author quark
 */
@XmlSeeAlso(value={Constant.class, Field.class, Site.class, UnrepresentableValue.class, List.class, Tuple.class, Tagged.class})
public abstract class Value {}
