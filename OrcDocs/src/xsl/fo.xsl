<?xml version='1.0' encoding="UTF-8"?>
<!-- fo.xsl - XSLT stylesheet - DocBook customization layer for Orc PDF documents

     Copyright (c) 2011 The University of Texas at Austin. All rights reserved.

     Use and redistribution of this file is governed by the license terms in
     the LICENSE file found in the project's top-level directory and also found at
     URL: http://orc.csres.utexas.edu/license.shtml .
  -->

<xsl:stylesheet xmlns:xslthl="http://xslthl.sf.net"
	exclude-result-prefixes="xslthl" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:od="http://orc.csres.utexas.edu/OrcDocBook.xsd" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

	<!-- Import unedited Docbook FO file -->
	<xsl:import href="../../docbook-xsl/fo/docbook.xsl"/>

	<!-- Import EBNF customization for FO -->
	<xsl:import href="ebnf-custom.xsl"/>

	<!-- Enable syntax highlighting of code elements -->
	<xsl:import href="highlighting.xsl"/>

	<xsl:output method="xml" indent="no"/>

	<xsl:attribute-set name="section.level1.properties">
		<xsl:attribute name="break-before">page</xsl:attribute>
	</xsl:attribute-set>

	<!-- Add site properties for the site library using the <sitepropset> tag -->
	<xsl:template match="od:sitepropset">
		<fo:block>
			<xsl:for-each select="od:siteprop">
				<xsl:if test="@propname='indefinite'">
					<fo:inline background-color="#CC0000" color="#FFFFFF"
						font-weight="bold" space-end="1em" padding="0.17em"> Indefinite </fo:inline>
				</xsl:if>
				<xsl:if test="@propname='definite'">
					<fo:inline background-color="#00CC00" color="#FFFFFF"
						font-weight="bold" space-end="1em" padding="0.17em"> Definite </fo:inline>
				</xsl:if>
				<xsl:if test="@propname='idempotent'">
					<fo:inline background-color="#0000CC" color="#FFFFFF"
						font-weight="bold" space-end="1em" padding="0.17em"> Idempotent </fo:inline>
				</xsl:if>
				<xsl:if test="@propname='pure'">
					<fo:inline background-color="#6600FF" color="#FFFFFF"
						font-weight="bold" space-end="1em" padding="0.17em"> Pure </fo:inline>
				</xsl:if>
			</xsl:for-each>
		</fo:block>
	</xsl:template>


	<xsl:template match='xslthl:literal' mode="xslthl">
		<xsl:apply-templates mode="xslthl"/>
	</xsl:template>

	<xsl:template match='xslthl:combinator' mode="xslthl">
		<fo:inline font-weight="bold">
			<xsl:apply-templates mode="xslthl"/>
		</fo:inline>
	</xsl:template>
	<xsl:template match='xslthl:combinator[text()="FATBAR"]'
		mode="xslthl">
		<fo:inline font-family="ZapfDingbats">&#x2759;</fo:inline>
	</xsl:template>

	<xsl:template match='xslthl:variable' mode="xslthl">
		<xsl:apply-templates mode="xslthl"/>
	</xsl:template>

	<xsl:template match='xslthl:site' mode="xslthl">
		<xsl:apply-templates mode="xslthl"/>
	</xsl:template>

</xsl:stylesheet>
