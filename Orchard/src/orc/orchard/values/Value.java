package orc.orchard.values;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.orchard.oil.Constant;
import orc.orchard.oil.Field;
import orc.orchard.oil.Site;

/**
 * Orc publishable values.
 * 
 * @author quark
 */
@XmlSeeAlso(value={Constant.class, Field.class, Site.class, UnrepresentableValue.class, List.class, Tuple.class})
public abstract class Value {}
