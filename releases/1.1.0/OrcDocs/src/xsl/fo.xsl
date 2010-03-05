<?xml version='1.0'?>
<xsl:stylesheet
    xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:import href="../../docbook-xsl/fo/docbook.xsl"/>
<xsl:import href="common.xsl"/>
<xsl:output method="xml" indent="no"/>

<xsl:template match='xslthl:literal' mode="xslthl">
  <xsl:apply-templates mode="xslthl"/>
</xsl:template>

<xsl:template match='xslthl:combinator' mode="xslthl">
  <fo:inline font-weight="bold"><xsl:apply-templates mode="xslthl"/></fo:inline>
</xsl:template>
<xsl:template match='xslthl:combinator[text()="FATBAR"]' mode="xslthl">
  <fo:inline font-family="ZapfDingbats">&#x2759;</fo:inline>
</xsl:template>

<xsl:template match='xslthl:variable' mode="xslthl">
  <xsl:apply-templates mode="xslthl"/>
</xsl:template>

<xsl:template match='xslthl:site' mode="xslthl">
  <xsl:apply-templates mode="xslthl"/>
</xsl:template>

</xsl:stylesheet>
