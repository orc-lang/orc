NECESSARY PROJECTS:

To properly build, you need the following projects from SVN:
  OrcDocgen
  OrcDocs
  OrcScala

TO BUILD THE REFERENCE MANUAL AND USER GUIDE:

(Note the manual only builds on Unix/Mac machines)

0) Enter the OrcDocs directory and type the following:
	
	ANT_OPTS=-Xmx2G ant all
	
   This increases the memory allotted for ANT to 2GB.
   
   Use the following targets for specific builds:
   ref-html-chunk		Chunked version of the reference manual
   ref-html-single		All-in-one-page version of the reference manual
   ref-pdf				PDF version of the reference manual
   targetdb				Update olink database files
   ug-html-chunk		Chunked version of the user guide
   ug-html-single		All-in-one-page version of the user guide
   ug-pdf				PDF version of the user guide
   


PROCEDURES FOR ADDING A NEW SECTION TO THE REFERENCE MANUAL/USER GUIDE:

0) Create and populate a new .xml document
      For example:
         "new.xml"  <-- Filled with reference content

1) Add an entity describing the filepath to the database
   for the new section in "olinkdb.xml"  Note this should
   match the 'targets.filename' expression in (1)
      For example:
         <!ENTITY targets.new SYSTEM "../build/targetdb/targets.new.xml">

2) Add a document representing the new section to the
   sitemap contained in "olinkdb.xml"  The 'targetdoc' will
   be used to refer to the document in olinks, and the
   'baseuri' must match the filename of the final HTML output.
   By default, this should be the 'xml:id' of the root tag of
   the new chapter/section plus '.html'
      For example:
         <document targetdoc="new" baseuri="new.html">
			&targets.new;
		 </document>

3) Add any necessary 'xinclude' statements to the parent
   .xml documents, i.e. make sure you've actually
   included your document in the build so the content is
   visible!
      For example:
         <xi:include href="new.xml"/> in "refmanual.xml"
         
4) Make sure the header to the .xml file is present and contains
   the necessary namespaces and inclusions.  Make sure to use
   xmlns:xi="http://www.w3.org/2001/XInclude" if you have any
   include statements in the document.
      For example:
         <!-- ug.language.orc.xml - DocBook source for the Orc user guide

         $Id$

         Copyright (c) 2011 The University of Texas at Austin. All rights reserved.

         Use and redistribution of this file is governed by the license terms in
         the LICENSE file found in the project's top-level directory and also found at
         URL: http://orc.csres.utexas.edu/license.shtml .
         -->

         <section xml:id="ug.language.orc"
		    xmlns="http://docbook.org/ns/docbook" 
		    xmlns:xlink="http://www.w3.org/1999/xlink"
		    xmlns:xi="http://www.w3.org/2001/XInclude">
		    
		    

PROCEDURES FOR LINKING BETWEEN PAGES WITHIN THE SAME DOCUMENT:

(I assume you have always followed the instructions above for your additional content)

0) Use an olink tag with the targetdoc as the xml:id of the section or
   chapter to which you are referring and a targetptr as the xml:id of
   the particular part of the document to which you are referring
      For example:
         <link linkend="ref.combinators.parallel.behavior">TEXT</link>
         
         
         
PROCEDURES FOR LINKING BETWEEN THE REFERENCE MANUAL AND THE USER GUIDE:

0) Use an olink tag with the targetdoc as the "root" document and a
   targetptr as the xml:id of the particular part of the document to
   which you are referring
      For example:
      	 -- link to all in one user guide
         <olink targetdoc="userguide" targetptr="section.orc.datatypes">TEXT</olink>
         -- link to chunked userguide
         <olink targetdoc="ug.language.orc" targetptr="orc.sites">TEXT</olink>



PROCEDURES FOR REMOVING A REFERENCE MANUAL/USER GUIDE SECTION

0) Delete all code related to the PROCEDURES FOR ADDING A NEW SECTION TO
THE REFERENCE MANUAL/USER GUIDE.  If you fail to do so, you'll get an error 
that looks something like this:

/filepath/olink.xsl:38:55: Warning! Can not load requested doc: /filepath/targets.ref.new.xml (No such file or directory)

This will likely break all links in the entire document (cascade of errors).
If you receive the above error, check to make sure "olinkdb.xml"
has been purged of all references to the "new.xml" based items.



GOOD RESOURCES:

The Complete Docbook Guide <http://www.sagehill.net/docbookxsl/>
