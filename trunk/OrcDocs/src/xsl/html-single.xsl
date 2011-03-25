<?xml version='1.0'?>
<xsl:stylesheet
    xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<!-- Import unedited docbook stylesheet -->
<xsl:import href="../../docbook-xsl/html/docbook.xsl"/>	

<!-- Enable DocBook syntax highlighting extension -->
<xsl:import href="../../docbook-xsl/html/highlight.xsl"/>

<!-- Enable syntax highlighting of code elements -->
<xsl:import href="highlighting.xsl"/>

<!-- Import customization layer -->
<xsl:import href="html-custom.xsl"/>
	
</xsl:stylesheet>
