<!-- test.xsl -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes"/>

  <xsl:template match="rootdoc">
	  <xsl:apply-templates/>
      <xsl:apply-templates select="document('test2.xml')"/>
	  <xsl:apply-templates select="document('test3.xml')"/>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
