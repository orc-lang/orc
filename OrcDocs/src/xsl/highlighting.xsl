<?xml version='1.0' encoding="UTF-8"?>
<!-- highlighting.xsl - XSLT stylesheet - DocBook customization layer for Orc syntax highlighting

     Copyright (c) 2011 The University of Texas at Austin. All rights reserved.

     Use and redistribution of this file is governed by the license terms in
     the LICENSE file found in the project's top-level directory and also found at
     URL: http://orc.csres.utexas.edu/license.shtml .
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:db="http://docbook.org/ns/docbook"
	version="1.0">

	<!-- Copied from http://xslthl.wiki.sourceforge.net/DocBook+XSL+Updates 
		to enable syntax highlighting of <code/> -->
	<xsl:template match="db:code">
		<xsl:variable name="content">
			<xsl:call-template name="apply-highlighting" />
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="count(ancestor::programlisting) &gt; 0">
				<xsl:copy-of select="$content" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="inline.monoseq">
					<xsl:with-param name="content" select="$content" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>