<!-- rm-includer.xsl -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes"/>
	
	<xsl:template match="include">
	 <xsl:apply-templates select="document(@href)/*"/>
	</xsl:template>

</xsl:stylesheet>
