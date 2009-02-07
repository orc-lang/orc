package orc.orchard.jmx;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

/**
 * Provide the name for an MBean operation parameter.
 * @author quark
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({PARAMETER})
public @interface JMXParam {
	String value();
}
