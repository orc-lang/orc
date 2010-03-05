<?xml version='1.0'?>
<xsl:stylesheet
    xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="common.xsl"/>

<xsl:param name="orc.demo" select="0"/>

<!--
Orc examples must use the "orc" CSS class.
-->
<xsl:template match="programlisting[@language='orc-demo']" mode="class.value">orc</xsl:template>

<!-- Include orc.js if desired -->
<xsl:template name="user.footer.content">
<xsl:if test="$orc.demo">
<script src="/orchard/orc.js" type="text/javascript"></script>
</xsl:if>
</xsl:template>

<!-- Include stylsheets, including orc.css if desired -->
<xsl:template name="user.head.content">
<xsl:if test="$orc.demo">
<link rel="stylesheet" type="text/css" href="/orchard/orc.css"/>
</xsl:if>
<link rel="stylesheet" type="text/css" href="style.css"/>
</xsl:template>

<!--
Customize syntax highlighting.
-->

<xsl:template match='xslthl:keyword' mode="xslthl">
  <span class="hl-keyword"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:comment' mode="xslthl">
  <span class="hl-comment"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:literal' mode="xslthl">
  <span class="hl-literal"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:combinator' mode="xslthl">
  <span class="hl-combinator"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>
<xsl:template match='xslthl:combinator[text()="FATBAR"]' mode="xslthl">
  <span class="hl-combinator">|</span>
</xsl:template>

<xsl:template match='xslthl:string' mode="xslthl">
  <span class="hl-string"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:variable' mode="xslthl">
  <span class="hl-variable"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:site' mode="xslthl">
  <span class="hl-site"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:doccomment' mode="xslthl">
  <span class="hl-comment"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

</xsl:stylesheet>
