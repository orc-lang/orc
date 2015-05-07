package orc.lib.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;

public class ReadText extends ThreadedSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			InputStreamReader in = (InputStreamReader)args.getArg(0);
			StringBuilder out = new StringBuilder();
			char[] buff = new char[1024];
			while (true) {
				int blen = in.read(buff);
				if (blen < 0) break;
				out.append(buff, 0, blen);
			}
			in.close();
			return out.toString();
		} catch (IOException e) {
			throw new JavaException(e);
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}
}
