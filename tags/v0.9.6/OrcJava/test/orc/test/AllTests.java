package orc.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import orc.test.parser.OrcParserTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	OrcParserTest.class,
	ExamplesTest.class
})
public class AllTests {
	public static void main(String[] args) {
		JUnitCore.main("orc.test.AllTests");
	}
}
