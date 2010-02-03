package org.jvnet.jax_ws_commons.json;

import java.util.HashSet;
import java.util.Set;

import com.sun.xml.internal.xsom.XSAnnotation;
import com.sun.xml.internal.xsom.XSAttGroupDecl;
import com.sun.xml.internal.xsom.XSAttributeDecl;
import com.sun.xml.internal.xsom.XSAttributeUse;
import com.sun.xml.internal.xsom.XSComplexType;
import com.sun.xml.internal.xsom.XSComponent;
import com.sun.xml.internal.xsom.XSContentType;
import com.sun.xml.internal.xsom.XSElementDecl;
import com.sun.xml.internal.xsom.XSFacet;
import com.sun.xml.internal.xsom.XSIdentityConstraint;
import com.sun.xml.internal.xsom.XSModelGroup;
import com.sun.xml.internal.xsom.XSModelGroupDecl;
import com.sun.xml.internal.xsom.XSNotation;
import com.sun.xml.internal.xsom.XSParticle;
import com.sun.xml.internal.xsom.XSSchema;
import com.sun.xml.internal.xsom.XSSimpleType;
import com.sun.xml.internal.xsom.XSType;
import com.sun.xml.internal.xsom.XSWildcard;
import com.sun.xml.internal.xsom.XSXPath;
import com.sun.xml.internal.xsom.visitor.XSVisitor;

/**
 * Visits the schema components.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SchemaWalker implements XSVisitor {

    private final Set<XSComponent> visited = new HashSet<XSComponent>();

    public void annotation(XSAnnotation ann) {
    }

    public void attGroupDecl(XSAttGroupDecl decl) {
        if(!visited.add(decl))   return;
        for( XSAttributeUse u : decl.getAttributeUses() )
            attributeUse(u);
    }

    public void attributeDecl(XSAttributeDecl decl) {
        if(!visited.add(decl))   return;
        simpleType(decl.getType());
    }

    public void attributeUse(XSAttributeUse use) {
        if(!visited.add(use))   return;
        attributeDecl(use.getDecl());
    }

    public void complexType(XSComplexType type) {
        if(!visited.add(type))   return;
        type.getContentType().visit(this);
        for( XSAttributeUse u : type.getAttributeUses() )
            attributeUse(u);
    }

    public void schema(XSSchema schema) {
        if(!visited.add(schema))   return;
        for (XSElementDecl e : schema.getElementDecls().values())
            elementDecl(e);
        for (XSAttributeDecl a : schema.getAttributeDecls().values())
            attributeDecl(a);
        for (XSAttGroupDecl a : schema.getAttGroupDecls().values())
            attGroupDecl(a);
        for (XSModelGroupDecl m : schema.getModelGroupDecls().values())
            modelGroupDecl(m);
        for (XSType t : schema.getTypes().values())
            t.visit(this);
        for (XSNotation n : schema.getNotations().values())
            notation(n);
    }

    public void facet(XSFacet facet) {
    }

    public void notation(XSNotation notation) {
    }

    public void identityConstraint(XSIdentityConstraint decl) {
    }

    public void xpath(XSXPath xp) {
    }

    public void wildcard(XSWildcard wc) {
    }

    public void modelGroupDecl(XSModelGroupDecl decl) {
        if(!visited.add(decl))   return;
        modelGroup(decl.getModelGroup());
    }

    public void modelGroup(XSModelGroup group) {
        if(!visited.add(group))   return;
        for (XSParticle p : group.getChildren())
            particle(p);
    }

    public void elementDecl(XSElementDecl decl) {
        if(!visited.add(decl))   return;
        decl.getType().visit(this);
    }

    public void simpleType(XSSimpleType simpleType) {
        if(!visited.add(simpleType))   return;
        simpleType.getBaseType().visit(this);
    }

    public void particle(XSParticle particle) {
        if(!visited.add(particle))   return;
        particle.getTerm().visit(this);
    }

    public void empty(XSContentType empty) {
    }
}
