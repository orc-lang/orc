package orc.orchard;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import orc.lib.orchard.forms.FormSenderSite;

@SuppressWarnings("serial")
public class FormsServlet extends HttpServlet {
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		FormSenderSite.service(request, response);
	}
}
