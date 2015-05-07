package orc.lib.orchard.forms;

import java.util.List;

import org.apache.commons.fileupload.FileItem;

public interface FormData {
	public FileItem getItem(String key);
	public FileItem[] getItems(String key);
	public List<FileItem> getItems();
	public String getParameter(String key);
	public String[] getParameterValues(String key);
}
