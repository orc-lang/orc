package orc.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * @author quark
 */
public class MakeDoc {
	private static class DocNodes {
		public String file;
		public List<DocNode> nodes;
		public DocNodes(String file, List<DocNode> nodes) {
			this.file = file;
			this.nodes = nodes;
		}
	}
	
	public static void main(String[] args) throws ParseException, IOException {
		List<DocNodes> files = new LinkedList<DocNodes>();
		for (String file : args) {
			files.add(new DocNodes(file, parseFile(file)));
		}
		for (DocNodes file : files) {
			System.out.println("<sect2><title>");
			System.out.println(escapeXML(file.file));
			System.out.println("</title>");
			for (DocNode doc : file.nodes) {
				System.out.println("<sect3><title>");
				System.out.println("<code>" + escapeXML(doc.type) + "</code>");
				System.out.println("</title>");
				System.out.print("<para>");
				System.out.print(doc.description.trim()
						.replaceAll("([ \t\f]*[\n\r]+){2,}", "</para>\n<para>"));
				System.out.println("</para></sect3>");
			}
			System.out.println("</sect2>");
		}
	}
	
	public static List<DocNode> parseFile(String file) throws ParseException, IOException {
		DocParser parser = new DocParser(
				new InputStreamReader(new FileInputStream(file)),
				file);
		Result result = parser.pContent(0);
		return (List<DocNode>)parser.value(result);
	}

	public static String escapeXML(String text) {
		StringBuilder sb = new StringBuilder();
		int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			switch (c) {
			case 34:
				sb.append("&quot;");
				break;
			case 38:
				sb.append("&amp;");
				break;
			case 39:
				sb.append("&apos;");
				break;
			case 60:
				sb.append("&lt;");
				break;
			case 62:
				sb.append("&gt;");
				break;
			default:
				if (c > 0x7F) {
					sb.append("&#");
					sb.append(Integer.toString(c, 10));
					sb.append(';');
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}
}