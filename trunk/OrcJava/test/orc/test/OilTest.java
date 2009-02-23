package orc.test;

import java.io.IOException;
import java.io.StringReader;

import orc.Config;
import orc.Orc;
import orc.ast.oil.Expr;
import orc.ast.oil.xml.Oil;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

import org.junit.Test;

public class OilTest {
	private String marshal() throws CompilationException, IOException {
		Config config = new Config();
		Oil oil = new Oil(Orc.compile(new StringReader("1"), config));
		return oil.toXML();
		// TODO: verify the syntax of the XML;
		// for now we just check for exceptions
	}
	@Test public void unmarshal() throws CompilationException, IOException {
		Oil oil = Oil.fromXML(marshal());
		oil.unmarshal();
		// TODO: verify the structure of the file
		// for now we just check for exceptions
	}
}
