package orc.test;

import orc.Config;
import junit.framework.Test;

/**
 * Run examples with type-checking enabled.
 * Not part of AllTests until the type checker
 * is more complete.
 * @author quark
 */
public class TypedExamplesTest {
	public static Test suite() {
		Config config = new Config();
		config.setTypeChecking(true);
		config.setIsolatedOn(true);
		return ExamplesTest.buildSuite(config);
	}
}
