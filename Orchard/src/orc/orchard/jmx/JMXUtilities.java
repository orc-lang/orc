package orc.orchard.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import orc.error.OrcError;

public final class JMXUtilities {
	private JMXUtilities() {}
	public static MBeanServer server = ManagementFactory.getPlatformMBeanServer(); 
	
	public static void unregisterMBean(ObjectName oname) {
		try {
			server.unregisterMBean(oname);
		} catch (InstanceNotFoundException e) {
			throw new OrcError(e);
		} catch (MBeanRegistrationException e) {
			throw new OrcError(e);
		}
	}
	public static ObjectName newObjectName(Object object, String id) {
		try {
			String name = object.getClass().getPackage().getName()
				+ ":type=" + object.getClass().getSimpleName()
				+ ",name=" + ObjectName.quote(id);
			return new ObjectName(name); 
		} catch (MalformedObjectNameException e) {
			throw new OrcError(e);
		}
	}
	public static <T> ObjectName registerMBean(T object, ObjectName oname) {
		try {
			server.registerMBean(new AnnotatedStandardMBean(object, null), oname);
			return oname;
		} catch (InstanceAlreadyExistsException e) {
			throw new OrcError(e);
		} catch (MBeanRegistrationException e) {
			throw new OrcError(e);
		} catch (NotCompliantMBeanException e) {
			throw new OrcError(e);
		}
	}
}
