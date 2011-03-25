<?xml version='1.0' encoding="UTF-8"?>
<!-- symbols.xsl - XSLT stylesheet - DocBook customization layer for Orc HTML documents

     $Id: html-custom.xsl 2606 2011-03-25 16:41:21Z jthywissen $

     Copyright (c) 2011 The University of Texas at Austin. All rights reserved.

     Use and redistribution of this file is governed by the license terms in
     the LICENSE file found in the project's top-level directory and also found at
     URL: http://orc.csres.utexas.edu/license.shtml .
  -->

<xsl:stylesheet xmlns:xslthl="http://xslthl.sf.net"
	exclude-result-prefixes="xslthl" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
<!-- Hackish ways of coercing FOP to render certain unicode charaters -->

<!-- ===================
	 Medium Vertical Bar
	 =================== -->
<xsl:template match="symbol[text() = '&#x2759;']">
  <fo:inline font-family="ZapfDingbats">
	&#x2759;
  </fo:inline>
</xsl:template>

<!-- ========================
	 Greater Than or Equal To
	 ======================== -->
<xsl:template match="symbol[text() = '&#x2265;']">
  <fo:inline font-family="Symbol">
	&#x2265;
  </fo:inline>
</xsl:template>

<!-- =====================
	 Less Than or Equal To
	 ===================== -->
<xsl:template match="symbol[text() = '≤']">
  <fo:inline font-family="Symbol">
	≤
  </fo:inline>
</xsl:template>

</xsl:stylesheet>