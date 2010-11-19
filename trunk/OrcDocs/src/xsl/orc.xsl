<?xml version='1.0'?>
<xsl:stylesheet
   version="1.0"  
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:fo="http://www.w3.org/1999/XSL/Format"
>

<!-- Standard docbook.xsl file -->
<xsl:import href="../../docbook-xsl/html/docbook.xsl"/>

<!-- ADD -->
	
<!-- Allows use of the <frag></frag> tag
	The tag is used for marking sections to copy
	from one document to another (a fragment) -->
<xsl:template match="frag">
  <xsl:apply-templates/>
</xsl:template>

<!-- DDA -->

</xsl:stylesheet>
