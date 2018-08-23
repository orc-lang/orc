//
// DotSite.java -- Java class DotSite
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.CallContext;
import orc.error.runtime.NoSuchMemberException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.values.FastRecord;
import orc.values.FastRecordFactory;
import orc.values.Field;
import orc.values.HasMembers;

/**
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using <code>addMembers()</code>. The code is forward-compatible with
 * many possible optimizations on the field lookup strategy. A dot site may also
 * have a default behavior which allows it to behave like a normal site. If its
 * argument is not a message, it displays that default behavior, if implemented.
 * If there is no default behavior, it raises a type error.
 *
 * @author dkitchin
 */
@Deprecated
public abstract class DotSite extends SiteAdaptor implements HasMembers {

    Map<String, Object> methodMap;

    public DotSite() {
        methodMap = new TreeMap<String, Object>();
        this.addMembers();
    }

    public static final boolean UseFastRecord = Boolean
                .parseBoolean(System.getProperty("orc.values.useFastRecord", "false"));

    public Object toFastRecord(FastRecordFactory factory) {
        if (!UseFastRecord) {
            return this;
        }
        ArrayList<Object> values = new ArrayList<>(methodMap.size());
        List<Field> expectedFields = Arrays.asList(factory.members());
        for (Field f : expectedFields) {
            Object v = methodMap.get(f.name());
            if (v == null) {
                throw new IllegalArgumentException("All factory fields must be in DotSite: " + f);
            }
            values.add(v);
        }
        return factory.apply(values.toArray());
    }

    @Override
    public void callSite(final Args args, final CallContext t) throws TokenException {

        String f;

        // Check if the argument is a message
        try {
            f = args.fieldName();
        } catch (final TokenException e) {
            // If not, invoke the default behavior and return.
            defaultTo(args, t);
            return;
        }

        // If it is a message, look it up.
        final Object m = getMember(f);
        if (m != null) {
            t.publish(object2value(m));
        } else {
            throw new NoSuchMemberException(this, f);
        }
    }

    Object getMember(final String f) {
        return methodMap.get(f);
    }

    protected abstract void addMembers();

    protected void addMember(final String f, final Object s) {
        methodMap.put(f, s);
    }

    protected void defaultTo(final Args args, final CallContext token) throws TokenException {
        throw new UncallableValueException("This dot site is not callable. It only has members.");
    }

    @Override
    public boolean nonBlocking() {
        return true;
    }

    @Override
    public Object getMember(Field f) {
        return getMember(f.name());
    }

    @Override
    public boolean hasMember(Field f) {
        return methodMap.containsKey(f.name());
    }
}
