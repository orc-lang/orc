package org.jvnet.jax_ws_commons.json.schema;

import org.jvnet.jax_ws_commons.json.SchemaConvention;
import org.jvnet.jax_ws_commons.json.SchemaWalker;

import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts XML Schema types to {@link JsonType JSON type}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JsonTypeBuilder {
    private final SchemaConvention convention;
    /**
     * Types that are already mapped.
     * <p>
     * To handle cyclic references correctly we need to remember
     * what we've already built. Such cycles only show up in
     * complex types, so those are the only ones we need to remember.
     */
    private final Map<XSType,JsonType> types = new HashMap<XSType,JsonType>();

    private int id=1;

    public JsonTypeBuilder(SchemaConvention convention) {
        this.convention = convention;
    }

    public JsonType create(XSType type) {
        JsonType jt = types.get(type);
        if(jt!=null)    return jt;

        if(type.isComplexType()) {
            final CompositeJsonType cjt = new CompositeJsonType(getTypeName(type));
            types.put(type,cjt);

            // fill in properties
            type.asComplexType().visit(new SchemaWalker() {
                boolean repeated = false;

                public void particle(XSParticle particle) {
                    boolean r = repeated;
                    repeated |= particle.isRepeated();
                    super.particle(particle);
                    repeated = r;
                }

                public void elementDecl(XSElementDecl decl) {
                    String j = convention.x2j.get(new QName(decl.getTargetNamespace(), decl.getName()));
                    if(cjt.properties.containsKey(j))
                        // this element shows up more than once.
                        cjt.properties.put(j, cjt.properties.get(j).makeArray());
                    else {
                        JsonType t = create(decl.getType());
                        if(repeated)    t=t.makeArray();
                        cjt.properties.put(j,t);
                    }
                }
            });
            return cjt;
        } else {
            XSSimpleType st = type.asSimpleType();
            if(st.getTargetNamespace().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                // built-in
                if(st.getName().equals("decimal")
                || st.getName().equals("float")
                || st.getName().equals("double")) {
                    return JsonType.NUMBER;
                }
                if(st.getName().equals("boolean"))
                    return JsonType.BOOLEAN;
                if(st.getName().equals("anySimpleType"))
                    return JsonType.STRING;
            }
            return create(type.getBaseType());
        }
    }

    private String getTypeName(XSType type) {
        if(type.isLocal())
            return "anonymousType#"+(id++);

        String n = type.getName();
        return Character.toUpperCase(n.charAt(0))+n.substring(1);
    }
}
