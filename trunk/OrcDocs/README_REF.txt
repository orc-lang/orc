TO BUILD THE REFERENCE MANUAL:

(Note it currently only builds on Unix machines)

0) Enter the OrcDocs directory and type the following:
	
	ANT_OPTS=-Xmx2G ant rm-build-html-chunk
	
   This increases the memory allotted for ANT to 2GB.
   Also, for a fresh build that updates the links, etc.
   build the following targets in order:
     clean
     olinks
     rm-build-html-chunk
   or use the 'test' target and make it depend on the above.



PROCEDURES FOR ADDING A NEW SECTION TO THE REFERENCE MANUAL:

0) Create and populate a new .xml document
      For example:
         "new.xml"  <-- Filled with reference content

1) Add an xslt instruction to "build.xml" to create an
   olinks database for the new file.  Use the filename
   in the 'include' section and the intended database
   (which should by convention be an expansion of the
   file base name in the format targets.FILE.db) for the 
   expression of the 'targets.filename' section.
      For example:
         <xslt style="${src.dir}/xsl/orc.xsl" 
				basedir="${src.dir}/refmanual" 
            destdir="${build.dir}/html/refmanual"
				processor="org.apache.tools.ant.taskdefs.optional.TraXLiaison">
			<include name="new.xml" />
			<classpath refid="xalan.classpath" />
			<param name="collect.xref.targets" expression="only" />
			<param name="targets.filename" expression="targets.new.db"/>
		</xslt>

2) Add an entity describing the filepath to the database
   for the new section in "olinkdb.xml"  Note this should
   match the 'targets.filename' expression in (1)
      For example:
         <!ENTITY targets.new SYSTEM "../build/html/refmanual/targets.new.db">

3) Add a document representing the new section to the
   sitemap contained in "olinkdb.xml"  The 'targetdoc' will
   be used to refer to the document in olinks, and the
   'baseuri' must match the filename of the final HTML output.
   By default, this should be the 'xml:id' of the root tag of
   the new chapter/section plus '.html'
      For example:
         <document targetdoc="new" baseuri="new.html">
				&targets.new;
			 </document>

4) Add any necessary 'xinclude' statements to the parent
   .xml documents, i.e. make sure you've actually
   included your document in the build so the content is
   visible!
      For example:
         <xi:include href="new.xml"/> in "mydocument.xml"
         


PROCEDURES FOR LINKING BETWEEN REFERENCE MANUAL PAGES:

(I assume you have always followed the instructions above for your additional content)

0) Use an olink tag with the targetdoc as the xml:id of the section or
   chapter to which you are referring and a targetptr as the xml:id of
   the particular part of the document to which you are referring
      For example:
         <olink targetdoc="ref.combinators" targetptr="ref.combinators">TEXT</olink>
         
         or
         
         <olink targetdoc="ref.combinators.parallel" targetptr="ref.combinators.parallel.behavior">TEXT</olink>
         
         
         
PROCEDURES FOR LINKING FROM THE REFERENCE MANUAL TO THE USER GUIDE:

(I assume you desire to link to the "all-in-one" single .html user guide)

0) Use an olink tag with the targetdoc as the "root" document and a
   targetptr as the xml:id of the particular part of the document to
   which you are referring
      For example:
         <olink targetdoc="root" targetptr="section.orc.datatypes">TEXT</olink>
         


PROCEDURES FOR REMOVING A REFERENCE MANUAL SECTION

0) Delete all code related to the PROCEDURES FOR ADDING A NEW SECTION TO
THE REFERENCE MANUAL.  If you fail to do so, you'll get an error that
looks something like this:

/filepath/olink.xsl:38:55: Warning! Can not load requested doc: /filepath/targets.ref.new.db (No such file or directory)

This will likely break all olinks in the entire document (cascade of errors).
If you receive the above error, check to make sure "build.xml" and "olinkdb.xml"
have been purged of all references to the "new.xml" based items.



GOOD RESOURCES:

The Complete Docbook Guide <http://www.sagehill.net/docbookxsl/>
