//
// FormSenderSite.java -- Java class FormSenderSite
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kilim.Mailbox;
import kilim.Pausable;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.orchard.OrchardProperties;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class FormSenderSite extends Site {
	public static class FormReceiver {
		private final Mailbox<Map<String, Object>> outbox = new Mailbox<Map<String, Object>>();
		private final Form form;
		private final String key;

		public FormReceiver(final OrcEngine globals, final Form form) {
			this.form = form;
			this.key = globals.addGlobal(this);
		}

		public String getURL() {
			return OrchardProperties.getProperty("orc.lib.orchard.forms.url") + "?k=" + key;
		}

		public Map<String, Object> get() throws Pausable {
			return outbox.get();
		}

		public void send() {
			outbox.putb(form.getValue());
		}
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		final OrcEngine engine = caller.getEngine();
		try {
			caller.resume(new FormReceiver(engine, (Form) args.getArg(0)));
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}

	private static void renderHeader(final PrintWriter out) throws IOException {
		// FIXME: remove this hard-coded URL
		out.write("<html><head></head>" + "<link rel='stylesheet' type='text/css' href='orc-forms.css'/>" + "<body>");
	}

	private static void renderFooter(final PrintWriter out) throws IOException {
		out.write("</body></html>");
	}

	private static void send(final HttpServletResponse response, final String message) throws IOException {
		final PrintWriter out = response.getWriter();
		renderHeader(out);
		out.write(message);
		renderFooter(out);
		out.close();
	}

	public static void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		FormData data;
		if (ServletFileUpload.isMultipartContent(request)) {
			try {
				data = new MultipartFormData(request);
			} catch (final FileUploadException e) {
				throw new ServletException(e);
			}
		} else {
			data = new PlainFormData(request);
		}

		final String key = data.getParameter("k");
		if (key == null) {
			send(response, "The URL is missing the required parameter 'k'.");
			return;
		}
		final FormReceiver f = (FormReceiver) OrcEngine.globals.get(key);
		if (f == null) {
			send(response, "The URL is no longer valid.");
			return;
		}
		final PrintWriter out = response.getWriter();
		final List<String> errors = new LinkedList<String>();

		// process request, if any
		if (data.getParameter("x") != null) {
			f.form.readRequest(data, errors);
			if (errors.isEmpty()) {
				OrcEngine.globals.remove(key);
				f.send();
				send(response, "Thank you for your response.");
				return;
			}
		}

		renderHeader(out);
		if (!errors.isEmpty()) {
			out.write("<ul>");
			for (final String error : errors) {
				out.write("<li>" + error + "</li>");
			}
			out.write("</ul>");
		}
		f.form.setHidden("k", f.key);
		f.form.setHidden("x", "1");
		f.form.render(out, new HashSet<String>());
		renderFooter(out);
		out.close();
	}
}
