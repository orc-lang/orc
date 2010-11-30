<?xml version='1.0'?>
<xsl:stylesheet
    xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	version="1.0">

<xsl:import href="common.xsl"/>

<xsl:param name="orc.demo" select="0"/>

<!--
Orc examples must use the "orc" CSS class.
-->
<xsl:template match="programlisting[@language='orc-demo']" mode="class.value">orc</xsl:template>

<!-- Include orc.js if desired and expand.js -->
<xsl:template name="user.footer.content">
<xsl:if test="$orc.demo">
<script src="/orchard/orc.js" type="text/javascript"></script>
</xsl:if>	
</xsl:template>
	
<xsl:template name="user.header.content">
	<script language="javascript">
	// Expandable content script from flooble.com.
	// For more information please visit:
	//   http://www.flooble.com/scripts/expand.php
	// Copyright 2002 Animus Pactum Consulting Inc.
	//----------------------------------------------
	var ie4 = false; if(document.all) { ie4 = true; }
	function getObject(id) { if (ie4) { return document.all[id]; } else { return document.getElementById(id); } }
	function toggle(link, divId) { var lText = link.innerHTML; var d = getObject(divId);
	  if (lText == '+') { link.innerHTML = '&#8722;'; d.style.display = 'block'; }
	  else { link.innerHTML = '+'; d.style.display = 'none'; } }
	</script>
</xsl:template>
	
<!-- Match template for collapsible example boxes -->
<xsl:template match="example">
	<xsl:variable name="ex_id" select="./@id"/>
	<xsl:variable name="ex_link" select="concat($ex_id,'_link')"/>
	<xsl:variable name="ex_content" select="concat($ex_id,'_content')"/>
	<!-- <xsl:variable name="ex_toggle" select="toggle(this, '$ex_content')"/> -->
	<!--
	<para>ex_id = <xsl:value-of select="$ex_id"/>  </para>
	<para>ex_link = <xsl:value-of select="$ex_link"/>  </para>
	<para>ex_content = <xsl:value-of select="$ex_content"/>  </para>
		-->
	<table border="0" width="60%" align="left"><tr><td>
	<div style="border: 1px solid #000000; padding: 0px; background: #FFFFFF; ">
		<table border="0" cellspacing="0" cellpadding="2" width="100%" style="background: #66CCFF; color: #000000; ">
			<tr><td><xsl:value-of select="./@caption"></xsl:value-of></td><td align="right">
			  [<a title="show/hide" href="javascript: void(0);" style="text-decoration: none; color: #000000; ">
				<xsl:attribute name="id"><xsl:value-of select="$ex_link" /></xsl:attribute>
				<xsl:attribute name="onclick">toggle(this, '<xsl:value-of select="$ex_content"/>')</xsl:attribute>+</a>]
			</td></tr>
		</table>
		<div style="padding: 3px;">
			<xsl:attribute name="id">
				<xsl:value-of select="$ex_content"/>
			</xsl:attribute>
			<xsl:apply-templates/>
		</div>
	</div>
	<!-- Start the box collapsed -->
	<script language="javascript">window.onload=toggle(this, '<xsl:value-of select="$ex_content"/>');</script>
	<noscript>
		<para>"WARNING:  This example requires javascript to be rendered correctly."</para>
		<xsl:apply-templates/>
	</noscript>
	</td></tr></table>
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
