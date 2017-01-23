//
// MultipartFormData.java -- Java class MultipartFormData
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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
    private final List<FileItem> items;

    public MultipartFormData(final List<FileItem> items) {
        this.items = items;
    }

    public MultipartFormData(final HttpServletRequest request) throws FileUploadException {
        final FileItemFactory factory = new DiskFileItemFactory();
        final ServletFileUpload upload = new ServletFileUpload(factory);
        this.items = upload.parseRequest(request);
    }

    @Override
    public FileItem getItem(final String key) {
        for (final FileItem item : items) {
            if (item.getFieldName().equals(key)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public FileItem[] getItems(final String key) {
        final LinkedList<FileItem> out = new LinkedList<FileItem>();
        for (final FileItem item : items) {
            if (item.getFieldName().equals(key)) {
                out.add(item);
            }
        }
        return out.toArray(new FileItem[0]);
    }

    @Override
    public List<FileItem> getItems() {
        return items;
    }

    @Override
    public String getParameter(final String key) {
        final FileItem item = getItem(key);
        if (item == null) {
            return null;
        }
        return item.getString();
    }

    @Override
    public String[] getParameterValues(final String key) {
        final FileItem[] items = getItems(key);
        final String[] out = new String[items.length];
        for (int i = 0; i < items.length; ++i) {
            out[i] = items[i].toString();
        }
        return out;
    }
}
