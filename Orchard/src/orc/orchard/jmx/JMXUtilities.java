//
// JMXUtilities.java -- Java class JMXUtilities
// Project Orchard
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public final class JMXUtilities {
    private JMXUtilities() {
    }

    public static MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    public static void unregisterMBean(final ObjectName oname) {
        try {
            server.unregisterMBean(oname);
        } catch (final InstanceNotFoundException e) {
            throw new AssertionError(e);
        } catch (final MBeanRegistrationException e) {
            throw new AssertionError(e);
        }
    }

    public static ObjectName newObjectName(final Object object, final String id) {
        try {
            final String name = object.getClass().getPackage().getName() + ":type=" + object.getClass().getSimpleName() + ",name=" + ObjectName.quote(id);
            return new ObjectName(name);
        } catch (final MalformedObjectNameException e) {
            throw new AssertionError(e);
        }
    }

    public static <T> ObjectName registerMBean(final T object, final ObjectName oname) {
        try {
            server.registerMBean(new AnnotatedStandardMBean(object, null), oname);
            return oname;
        } catch (final InstanceAlreadyExistsException e) {
            throw new AssertionError(e);
        } catch (final MBeanRegistrationException e) {
            throw new AssertionError(e);
        } catch (final NotCompliantMBeanException e) {
            throw new AssertionError(e);
        }
    }
}
