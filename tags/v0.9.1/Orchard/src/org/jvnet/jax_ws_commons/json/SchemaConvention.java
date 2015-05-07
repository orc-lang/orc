package org.jvnet.jax_ws_commons.json;

import com.sun.xml.bind.api.impl.NameConverter;
import org.codehaus.jettison.Convention;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Convention} implementation that works with {@link MappedXMLStreamReader}
 * and {@link MappedXMLStreamWriter}.
 *
 * <p>
 * This uses a set of known tag names so that each QName maps to a natural
 * JSON property names.
 *  
 * @author Kohsuke Kawaguchi
 */
public class SchemaConvention extends MappedNamespaceConvention {
    /**
     * Tag names -> JSON names conversion.
     */
    public final Map<QName,String> x2j = new HashMap<QName,String>();

    /**
     * JSON names -> tag names conversion.
     */
    public final Map<String,QName> j2x = new HashMap<String,QName>();

    /**
     * ORC: hack to work around problems interpreting xsi namespace
     * in OIL XML format.
     * @return
     */
    private static Configuration getDefaultConfiguration() {
    	Map<String, String> xmlToJsonNamespaces = new HashMap<String, String>();
    	xmlToJsonNamespaces.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
    	Configuration out = new Configuration(xmlToJsonNamespaces);
    	return out;
    }
    
    public SchemaConvention(Collection<QName> tagNames) {
    	super(getDefaultConfiguration());
        // sort them in a consistent order so that mapping remains stable
        QName[] names = tagNames.toArray(new QName[tagNames.size()]);
        Arrays.sort(names, QNAME_SORTER);

        OUTER:
        for (QName n : names) {
            if(!j2x.containsKey(n.getLocalPart())) {
                // try to use just the local name
                register(n.getLocalPart(),n);
                continue;
            }

            String token = getLastNsToken(n);
            if(token!=null) {
                // try to use the prefix of the token
                for( int i=1; i<token.length(); i++ ) {
                    String jsonName = token.substring(0,i)+'_'+n.getLocalPart();
                    if(!j2x.containsKey(jsonName)) {
                        register(jsonName,n);
                        continue OUTER;
                    }
                }
            }

            // none worked. last resort.
            int i=2;
            while(j2x.containsKey(n.getLocalPart()+i))
                i++;
            register(n.getLocalPart()+i,n);
        }
    }

    private void register(String jsonName, QName qName) {
        j2x.put(jsonName,qName);
        x2j.put(qName,jsonName);
    }

    /**
     * Return a "token" from namespace URI.
     *
     * A token is a part of the namespace URI that hopefully would have some
     * meaning to humans.
     *
     * @return null if cannot be found.
     */
    private String getLastNsToken(QName n) {
        String[] tokens = n.getNamespaceURI().split("[/.:]");
        for( int i=tokens.length-1; i>=0; i-- ) {
            if(tokens[i].length()!=0)
                return NameConverter.standard.toVariableName(tokens[i]);
        }
        return null;
    }


    public QName createQName(String rootName, Node node) {
        QName qn = j2x.get(rootName);
        if(qn!=null)    return qn;

        // we are seeing a tag name that was not in the schema. wildcard, etc.
        return new QName(rootName);
    }

    /**
     * Convert XML name to JSON.
     */
    public String createKey(String p, String ns, String local) {
        String json = x2j.get(new QName(ns, local));
        if(json!=null)  return json;

        // we are seeing a tag name that was not in the schema. wildcard, etc.
        return local;
    }

    public boolean isElement(String p, String ns, String local) {
        return false;
    }

    private static final Comparator<QName> QNAME_SORTER = new Comparator<QName>() {
        public int compare(QName lhs, QName rhs) {
            int r = lhs.getNamespaceURI().compareTo(rhs.getNamespaceURI());
            if(r!=0)    return r;
            return lhs.getLocalPart().compareTo(rhs.getLocalPart());
        }
    };
}
