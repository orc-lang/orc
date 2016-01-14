<?xml version='1.0' encoding="UTF-8"?>
<!-- html-single.xsl - XSLT stylesheet - DocBook customization layer for Orc

     Copyright (c) 2011 The University of Texas at Austin. All rights reserved.

     Use and redistribution of this file is governed by the license terms in
     the LICENSE file found in the project's top-level directory and also found at
     URL: http://orc.csres.utexas.edu/license.shtml .
  -->

<xsl:stylesheet xmlns:xslthl="http://xslthl.sf.net"
	exclude-result-prefixes="xslthl" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

	<!-- Import unedited docbook stylesheet -->
	<xsl:import href="../../docbook-xsl/html/docbook.xsl" />

	<!-- Enable DocBook syntax highlighting extension -->
	<xsl:import href="../../docbook-xsl/html/highlight.xsl" />

	<!-- Enable syntax highlighting of code elements -->
	<xsl:import href="highlighting.xsl" />

	<!-- Import customization layer -->
	<xsl:import href="html-custom.xsl" />

</xsl:stylesheet>
