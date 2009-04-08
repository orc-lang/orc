package orc.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.ho.yaml.YamlDecoder;

/**
 * Collect third party licenses into a single file.
 * @author quark
 */
public class ThirdPartyLicenses {
	private ThirdPartyLicenses() {}
	public static void main(String[] args) throws IOException {
		YamlDecoder dec = new YamlDecoder(new FileInputStream("licenses.yml"));
		FileWriter out = new FileWriter("THIRD_PARTY_LICENSES.txt");
		try {
			boolean notFirst = false;
			while (true) {
				notFirst = true;
				Map dep = (Map)dec.readObject();
				String project = (String)dep.get("project");
				String url = (String)dep.get("url");
				String phase = (String)dep.get("phase");
				if (phase.equals("build")) continue;
				List<String> licenses = (List<String>)dep.get("licenses");
				out.write("+++ This project incorporates one or more files from " + project + " (" + url + ") under the following license(s):\r\n\r\n");
				for (String license : licenses) {
					BufferedReader in = new BufferedReader(new FileReader("licenses/" + license));
					try {
						String line;
						while (null != (line = in.readLine())) {
							out.write(line);
							out.write("\r\n");
						}
						out.write("\r\n\r\n");
					} finally {
						in.close();
					}
				}
			}
		} catch (EOFException _) {
			// do nothing
		} finally {
			dec.close();
			out.close();
		}
	}
}
