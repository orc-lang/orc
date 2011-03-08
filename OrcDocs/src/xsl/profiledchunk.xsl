<?xml version='1.0'?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

<!-- Unedited docbook .xsl (need profiling for index)-->
<xsl:import href="../../docbook-xsl/html/profile-chunk.xsl"/>

<!-- Each firstterm is placed into index as a bolded, primary term-->
<xsl:template match="firstterm" mode="profile">
  <!-- Copy original element -->
  <xsl:copy-of select="."/>
  <!-- Create new index entry -->
  <indexterm significance="preferred"> <!-- firstterms are bold -->
    <primary><xsl:value-of select="."/></primary>
  </indexterm>
</xsl:template>

<!-- <ind> tag for words we want in index but not firstterms -->
<xsl:template match="ind" mode="profile">
  <!-- Copy original element's text content -->
  <xsl:value-of select="."/>
  <!-- Create new index entry -->
  <indexterm>
    <primary><xsl:value-of select="."/></primary>
  </indexterm>
</xsl:template>

</xsl:stylesheet>