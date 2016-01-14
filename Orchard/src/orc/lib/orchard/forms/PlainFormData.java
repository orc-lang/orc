//
// PlainFormData.java -- Java class PlainFormData
// Project Orchard
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

public class PlainFormData implements FormData {
    private final HttpServletRequest request;

    public PlainFormData(final HttpServletRequest request) {
        super();
        this.request = request;
    }

    @Override
    public FileItem getItem(final String key) {
        return new PlainFileItem(key, request.getParameter(key));
    }

    @Override
    public FileItem[] getItems(final String key) {
        final String[] values = request.getParameterValues(key);
        final FileItem[] out = new FileItem[values.length];
        for (int i = 0; i < values.length; ++i) {
            out[i] = new PlainFileItem(key, values[i]);
        }
        return out;
    }

    @Override
    public List<FileItem> getItems() {
        final LinkedList<FileItem> out = new LinkedList<FileItem>();
        for (final Enumeration<String> names = request.getParameterNames(); names.hasMoreElements();) {
            final String name = names.nextElement();
            for (final String value : request.getParameterValues(name)) {
                out.add(new PlainFileItem(name, value));
            }
        }
        return out;
    }

    @Override
    public String getParameter(final String key) {
        return request.getParameter(key);
    }

    @Override
    public String[] getParameterValues(final String key) {
        return request.getParameterValues(key);
    }
}

class PlainFileItem implements FileItem {
    private String name;
    private final String value;

    public PlainFileItem(final String name, final String value) {
        super();
        this.name = name;
        this.value = value;
    }

    @Override
    public void delete() {
        // do nothing
    }

    @Override
    public byte[] get() {
        return value.getBytes();
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public String getFieldName() {
        return name;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(value.getBytes());
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public long getSize() {
        return value.getBytes().length;
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public String getString(final String arg0) throws UnsupportedEncodingException {
        return value;
    }

    @Override
    public boolean isFormField() {
        return false;
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public void setFieldName(final String arg0) {
        name = arg0;
    }

    @Override
    public void setFormField(final boolean arg0) {
        // do nothing
    }

    @Override
    public void write(final File arg0) throws Exception {
        throw new UnsupportedOperationException("write not supported for plain form data");
    }
}
