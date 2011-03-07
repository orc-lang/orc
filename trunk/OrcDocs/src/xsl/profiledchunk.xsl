<?xml version='1.0'?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

<!-- Unedited docbook .xsl (need profiling for index)-->
<xsl:import href="../../docbook-xsl/html/profile-chunk.xsl"/>

<!-- Each firstterm is placed into index -->
<xsl:template match="firstterm" mode="profile">
  <!-- Copy original element -->
  <xsl:copy-of select="."/>
  <!-- Create new index entry -->
  <indexterm>
    <primary><xsl:value-of select="."/></primary>
  </indexterm>
</xsl:template>

</xsl:stylesheet>