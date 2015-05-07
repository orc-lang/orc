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
		System.out.println("<?xml version=\"1.0\"?>");
		System.out.println("<section><title>Reference</title>");
		int anchor;
		anchor = 0;
		for (DocNodes file : files) {
			System.out.print("<section><title>");
			System.out.print(escapeXML(file.file));
			if (!file.nodes.isEmpty()) {
				DocNode first = file.nodes.get(0);
				if (first instanceof DocParagraph) {
					System.out.print(": ");
					System.out.print(firstSentence(((DocParagraph)first).body.trim()));
				}
			}
			System.out.println("</title>");
			int depth = 0;
			for (DocNode doc : file.nodes) {
				if (doc instanceof DocParagraph) {
					System.out.print("<para>");
					System.out.print(((DocParagraph)doc).body.trim());
					System.out.println("</para>");
				} else {
					DocType type = (DocType)doc;
					if (type.depth > depth) {
						System.out.println("<variablelist>");
						depth = type.depth;
					} else {
						while (type.depth < depth) {
							System.out.println("</listitem></varlistentry></variablelist>");
							--depth;
						}
						if (type.depth == depth) {
							System.out.println("</listitem></varlistentry>");
						}
						depth = type.depth;
					}
					System.out.print("<varlistentry><term>");
					System.out.print("<code>" + escapeXML(extractName(type.type)) + "</code>");
					System.out.print("</term><listitem>");
					System.out.print("<para>");
					System.out.print("<code>" + escapeXML(type.type) + "</code>");
					System.out.println("</para>");
				}
			}
			while (0 < depth) {
				System.out.println("</listitem></varlistentry></variablelist>");
				--depth;
			}
			System.out.println("</section>");
		}
		System.out.println("</section>");
	}
	
	public static String extractName(String type) {
		// extract the declaration name, which follows the
		// declaration keyword and preceeds the argument list
		String out = type.replaceAll("[a-z]+\\s+(.[^(]+)\\(.*", "$1");
		// drop the type part of a method prefix, if necessary
		return out.replaceFirst("^[^.]+\\.", "");
	}
	
	public static String firstSentence(String para) {
		String[] parts = para.split("(?<=[.?!])\\s+", 2);
		return parts[0];
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
