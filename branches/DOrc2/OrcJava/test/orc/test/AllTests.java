package orc.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	OrcParserTest.class,
	ExamplesTest.class,
	OilTest.class,
	IntervalsTest.class
})
public class AllTests {
	public static void main(String[] args) {
		JUnitCore.main("orc.test.AllTests");
	}
}
