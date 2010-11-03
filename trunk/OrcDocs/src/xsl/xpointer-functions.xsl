<?xml version='1.0'?>
<!-- Copyright (c) 2002 ISOGEN International 

     This style sheet fragment implements the resolution of xpointers.
     It requires implementation of the exslt common and functions 
     modules.

     Author: W. Eliot Kimber, eliot@isogen.com

     $Revision: 1.1 $
  -->
<!-- Status:

  -->
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/TR/xlink"
  xmlns:xptr="http://www.w3.org/2001/05/XPointer"
  xmlns:xptrf="http://www.isogen.com/functions/xpointer"
  xmlns:saxon="http://icl.com/saxon"
  xmlns:func="http://exslt.org/functions"
  xmlns:fcommon="http://exslt.org/common"
  extension-element-prefixes="func xptr"
>

<func:function name="xptrf:resolve-xpointer-url">
  <!-- Given an element that exhibits an href attribute,
       attempts to resolve the URL and XPointer (if present)
       into a result node list.

       If there is no fragment identifier, acts as though
       the fragment identifier "#/" had been specified,
       returning the document root.
    -->
  <xsl:param name="pointer-node"/>
  <!-- The Element node that exhibits the XPointer to be resolved -->
  <xsl:variable name="href" select="$pointer-node/@href"/>
  <xsl:choose>
    <xsl:when test="starts-with($href,'#')">
      <xsl:variable name="fragid">
        <xsl:value-of select="substring($href, 2)"/>
      </xsl:variable>
      <xsl:variable name="xpointer" select="xptrf:fragid2xpointer($fragid)"/>
      <!-- NOTE: error checking and reporting is done by resolve-xpointer -->
      <xsl:variable name="rns" 
      select="xptrf:resolve-xpointer($pointer-node, $xpointer)"/>
      <xsl:choose>
        <xsl:when test="string($rns) = ''">
          <func:result select="/.."/>
        </xsl:when>
        <xsl:when test="fcommon:object-type($rns) != 'node-set'">
          <func:result select="/.."/>
        </xsl:when>
        <xsl:otherwise>
          <func:result select="$rns"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="url">
        <xsl:variable name="cand-url" 
        select="substring-before($pointer-node/@href, '#')"/>
        <xsl:choose>
          <xsl:when test="$cand-url = ''">
            <xsl:value-of select="$href"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$cand-url"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="cand-xpointer">
        <xsl:value-of select="substring-after($href, '#')"/>
      </xsl:variable>
      <xsl:variable name="xpointer">
        <xsl:choose>
          <xsl:when test="$cand-xpointer = ''">
            <xsl:value-of select="string('/')"/>
            <!-- Return the document element of the target document -->        
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="xptrf:fragid2xpointer($cand-xpointer)"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="location-source-node" 
       select="document($url, $pointer-node)"/>
      <xsl:variable name="rns" 
       select="xptrf:resolve-xpointer($location-source-node, 
                                           $xpointer)"/>
      <xsl:choose>
        <xsl:when test="string($rns) = ''">
          <func:result select="/.."/>
        </xsl:when>
        <xsl:when test="fcommon:object-type($rns) != 'node-set'">
          <func:result select="/.."/>
        </xsl:when>
        <xsl:otherwise>
          <func:result select="$rns"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>

</func:function>

<func:function name="xptrf:resolve-xpointer">
  <!-- Resolves an xpointer in the context of some location source node.

       The location source is either the pointer, if the URL was just
       an XPointer, or it's the document element of the document addressed by the 
       resource part of the URL.
    -->
  <xsl:param name="location-source-node"/>
  <xsl:param name="xpointer"/>
  <xsl:for-each select="$location-source-node">
    <!-- Setting the context to the pointer node so that relative URLs are resolved
         relative to the pointer node by saxon:evaluate() -->
    <xsl:choose>
      <xsl:when test="$xpointer != ''">     
        <xsl:variable name="direct-result-set" select="saxon:evaluate($xpointer)"/>
        <xsl:choose>
          <xsl:when test="string($direct-result-set) = ''">
            <xsl:message>XIndirect warning: XPointer "<xsl:value-of 
            select="$xpointer"/>" did not address any nodes.</xsl:message>
          </xsl:when>
          <xsl:when test="fcommon:object-type($direct-result-set) != 'node-set'">
            <xsl:message>XIndirect warning: XPointer "<xsl:value-of 
             select="$xpointer"/>" did not address any nodes.</xsl:message>
            <func:result select="/.."/>
          </xsl:when>
          <xsl:otherwise>
            <func:result select="$direct-result-set"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message>XPointer error: $xpointer value is '' in resolve-xpointer.
        </xsl:message>
        <func:result select="/.."/>
      </xsl:otherwise>
    </xsl:choose>  
  </xsl:for-each>

</func:function>

<func:function name="xptrf:fragid2xpointer">
  <xsl:param name="fragid"/>
  <!-- Given a fragment identifier string, attempts to interpret it as an XPointer. -->
  <!-- NOTE: does not:
       - Handle multi-part XPointers: "#xpointer(foo)xpointer(bar)"
       - Skip non-xpointer schemes

       Doing this would require more sophisticated string processing than I can
       reasonably do in XSLT.
    -->
  <xsl:choose>
    <xsl:when test="starts-with($fragid, 'xpointer(')">
      <xsl:variable name="first-part" select="substring-after($fragid, 'xpointer(')"/>
      <xsl:variable name="len" select="(string-length($first-part) - 1)"/>
      <xsl:variable name="xpointer" select="substring($first-part,1,$len)"/>
      <func:result select="$xpointer"/>
    </xsl:when>
    <xsl:when test="not(contains($fragid, '/')) and
                    not(contains($fragid, '[')) and
                    not(contains($fragid, '*')) and
                    not(contains($fragid, '@'))">
      <!-- Probably a bare name -->
      <func:result select="concat('id(', $fragid, ')')"/>
    </xsl:when>
    <xsl:when test="contains($fragid, '/') and
                    not(contains($fragid, '[')) and
                    not(contains($fragid, '*')) and
                    not(contains($fragid, '@'))">
      <!-- Probably a child sequence -->
      <xsl:variable name="barename" select="substring-before($fragid, '/')"/>
      <xsl:choose>
        <xsl:when test="$barename = '' and
                        contains(translate($fragid, 
'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ',
'^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^'),
                                 '^')">
          <func:result select="xptrf:xpointer-error($fragid)"/>
        </xsl:when>
        <xsl:when test="$barename = ''">
          <xsl:message>fragid='<xsl:value-of select="$fragid"/>'</xsl:message>
          <xsl:variable name="childseq" 
              select="xptrf:build-child-sequence($fragid)"/>
          <func:result select="$childseq"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="idref" select="concat('id(', $barename, ')')"/>
          <xsl:variable name="xpointer-childseq" 
              select="substring($fragid, (string-length($barename) + 1))"/>
          <xsl:choose>
            <xsl:when test="contains(
translate($xpointer-childseq, 
'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ',
'^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^'),
                                     '^')">
             <func:result select="xptrf:xpointer-error($fragid)"/>
            </xsl:when>
            <xsl:otherwise>
              <func:result select="concat('id(', $barename, ')', 
                         xptrf:build-child-sequence($xpointer-childseq))"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <func:result select="xptrf:xpointer-error($fragid)"/>
    </xsl:otherwise>
  </xsl:choose>
</func:function>

<func:function name="xptrf:build-child-sequence">
  <xsl:param name="xptr-childseq"/>
  <xsl:choose>
    <xsl:when test="not(starts-with($xptr-childseq, '/'))">
      <func:result select="xptrf:xpointer-error($xptr-childseq)"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="temp" select="substring($xptr-childseq, 2)"/>
      <!-- strip leading "/" -->
      <func:result select="xptrf:construct-child-sequence($temp)"/>
    </xsl:otherwise>
  </xsl:choose>
</func:function>

<func:function name="xptrf:construct-child-sequence">
  <xsl:param name="xptr-child-seq"/>
  <xsl:param name="xpath-child-seq"/>
  <xsl:variable name="child-num">
    <xsl:choose>
      <xsl:when test="contains($xptr-child-seq, '/')">
        <xsl:value-of select="concat('/*[', 
        substring-before($xptr-child-seq, '/'), ']')"/>
      </xsl:when>
       
      <xsl:otherwise>
        <xsl:value-of select="$xptr-child-seq"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="rest" select="substring-after($xptr-child-seq, '/')"/>
  <xsl:choose>
    <xsl:when test="$rest = ''">
      <func:result select="$xpath-child-seq"/>
    </xsl:when>
    <xsl:otherwise>
      <func:result select="xptrf:construct-child-sequence(
      $rest, concat($xpath-child-seq, $child-num))"/>
    </xsl:otherwise>
  </xsl:choose>
</func:function>

<func:function name="xptrf:xpointer-error">
  <!-- Reports an XPointer error and returns "/.." -->
  <xsl:param name="fragid"/>
  <xsl:message
>XPointer error: fragment identifier "<xsl:value-of 
select="$fragid"/>" is not a valid XPointer.
                Returning "/.." as XPath to resolve (empty node set)</xsl:message>
  <func:result select="concat('/', '..')"/>
</func:function>
</xsl:stylesheet>

