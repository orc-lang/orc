package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kilim.Mailbox;
import kilim.Pausable;

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.orchard.Job;
import orc.orchard.Job.ProvidesGlobals;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

public class FormSenderSite extends Site {
	public static class FormReceiver {
		private Mailbox<Map<String, Object>> outbox = new Mailbox<Map<String, Object>>();
		private Form form;
		private String key;
		public FormReceiver(ProvidesGlobals globals, Form form) {
			this.form = form;
			this.key = globals.addGlobal(this);
		}
		public String getURL() {
			// FIXME: remove this hard-coded URL
			return "http://orc.csres.utexas.edu/orchard/FormsServlet?k=" + key;
		}
		public Map<String, Object> get() throws Pausable {
			return outbox.get();
		}
		public void send() {
			outbox.putb(form.getValue());
		}
	}
	
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		OrcEngine engine = caller.getEngine();
		if (!(engine instanceof ProvidesGlobals)) {
			throw new SiteException(
					"This site is not supported on the engine " +
					engine.getClass().toString());
		}
		try {
			caller.resume(new FormReceiver((ProvidesGlobals)engine, (Form)args.getArg(0)));
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}
		
	private static void renderHeader(PrintWriter out) throws IOException {
		// FIXME: remove this hard-coded URL
		out.write("<html><head></head>" +
				"<link rel='stylesheet' type='text/css' href='orc-forms.css'/>"+
				"<body>");
	}
	
	private static void renderFooter(PrintWriter out) throws IOException {
		out.write("</body></html>");
	}
	
	private static void send(HttpServletResponse response, String message) throws IOException {
		PrintWriter out = response.getWriter();
		renderHeader(out);
		out.write(message);
		renderFooter(out);
		out.close();
	}
	
	public static void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String key = request.getParameter("k");
		if (key == null) {
			send(response, "The URL is missing the required parameter 'k'.");
			return;
		}
		FormReceiver f = (FormReceiver)Job.globals.get(key);
		if (f == null) {
			send(response, "The URL is no longer valid.");
			return;
		}
		PrintWriter out = response.getWriter();
		List<String> errors = new LinkedList<String>();
		
		// process request, if any
		if (request.getParameter("x") != null) {
			f.form.readRequest(request, errors);
			if (errors.isEmpty()) {
				Job.globals.remove(key);
				f.send();
				send(response, "Thank you for your response.");
				return;
			}
		}
		
		renderHeader(out);
		if (!errors.isEmpty()) {
			out.write("<ul>");
			for (String error : errors) {
				out.write("<li>" + error + "</li>");
			}
			out.write("</ul>");
		}
		f.form.setHidden("k", f.key);
		f.form.setHidden("x", "1");
		f.form.render(out);
		renderFooter(out);
		out.close();
	}
}