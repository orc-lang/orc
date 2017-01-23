//
// FormData.java -- Java interface FormData
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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
