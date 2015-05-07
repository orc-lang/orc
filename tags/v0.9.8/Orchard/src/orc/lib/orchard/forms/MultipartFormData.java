package orc.lib.orchard.forms;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class MultipartFormData implements FormData {
	private List<FileItem> items;
	
	public MultipartFormData(List<FileItem> items) {
		this.items = items;
	}
	
	public MultipartFormData(HttpServletRequest request) throws FileUploadException {
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		this.items = upload.parseRequest(request);
	}

	public FileItem getItem(String key) {
		for (FileItem item : items) {
			if (item.getFieldName().equals(key)) return item;
		}
		return null;
	}

	public FileItem[] getItems(String key) {
		LinkedList<FileItem> out = new LinkedList<FileItem>();
		for (FileItem item : items) {
			if (item.getFieldName().equals(key)) out.add(item);
		}
		return out.toArray(new FileItem[0]);
	}

	public List<FileItem> getItems() {
		return items;
	}

	public String getParameter(String key) {
		FileItem item = getItem(key);
		if (item == null) return null;
		return item.getString();
	}

	public String[] getParameterValues(String key) {
		FileItem[] items = getItems(key);
		String[] out = new String[items.length];
		for (int i = 0; i < items.length; ++i) {
			out[i] = items[i].toString();
		}
		return out;
	}
}
