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
	@Test public void unmarshal() throws CompilationException, IOException {
		Config config = new Config();
		Oil oil1 = new Oil(Orc.compile(new StringReader("1"), config));
		String xml = oil1.toXML();
		System.out.println(xml);
		// TODO: verify the syntax of the XML;
		// for now we just check for exceptions
		Oil oil2 = Oil.fromXML(xml);
		oil2.unmarshal(config);
		// TODO: verify the structure of the file
		// for now we just check for exceptions
	}
}
