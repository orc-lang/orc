package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.fileupload.FileItem;


public class UploadField extends Field<FileItem> {
	public UploadField(String key, String label) {
		super(key, label, null);
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='file' name='"+key+"'>");
	}
	
	public boolean needsMultipartEncoding() {
		return true;
	}

	public void readRequest(FormData request, List errors) {
		value = request.getItem(key);
	}
}
