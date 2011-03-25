<?xml version='1.0'?>
<xsl:stylesheet
    xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="../../docbook-xsl/fo/docbook.xsl"/>
<xsl:output method="xml" indent="no"/>

<!-- Enable DocBook syntax highlighting extension -->
<xsl:import href="../../docbook-xsl/fo/highlight.xsl"/>

<!-- Enable syntax highlighting of code elements -->
<xsl:import href="highlighting.xsl"/>

<xsl:attribute-set name="section.level1.properties">
  <xsl:attribute name="break-before">page</xsl:attribute>
</xsl:attribute-set>


<!--  Add site properties for the site library using the <sitepropset> tag -->
<xsl:template match="sitepropset">
	<fo:block>
   		<xsl:for-each select="siteprop">
   			<xsl:if test="@propname='indefinite'">
   				<fo:inline background-color="#CC0000" color="#FFFFFF" space-end="1em" padding="0.17em"> Indefinite </fo:inline>
   			</xsl:if>
   			<xsl:if test="@propname='definite'">
   				<fo:inline background-color="#00CC00" color="#FFFFFF" space-end="1em" padding="0.17em"> Definite </fo:inline>
   			</xsl:if>
   			<xsl:if test="@propname='idempotent'">
   				<fo:inline background-color="#0000CC" color="#FFFFFF" space-end="1em" padding="0.17em"> Idempotent </fo:inline>
   			</xsl:if>
   			<xsl:if test="@propname='pure'">
   				<fo:inline background-color="#6600FF" color="#FFFFFF" space-end="1em" padding="0.17em"> Pure </fo:inline>
   			</xsl:if>
		</xsl:for-each>
	</fo:block>
</xsl:template>


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
	
<xsl:template match='examplescript'>
	<!-- Ignore <examplescript> tags -->
</xsl:template>

</xsl:stylesheet>
