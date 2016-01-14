<?xml version='1.0' encoding="UTF-8"?>
<!-- html-chunk.xsl - XSLT stylesheet - DocBook customization layer for Orc chunked HTML documents

     Copyright (c) 2011 The University of Texas at Austin. All rights reserved.

     Use and redistribution of this file is governed by the license terms in
     the LICENSE file found in the project's top-level directory and also found at
     URL: http://orc.csres.utexas.edu/license.shtml .
  -->

<xsl:stylesheet xmlns:xslthl="http://xslthl.sf.net"
	exclude-result-prefixes="xslthl" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

	<!-- Import profiling and chunking (need profiling for index) -->
	<xsl:import href="../../docbook-xsl/html/profile-chunk.xsl" />

	<!-- Enable DocBook syntax highlighting extension -->
	<xsl:import href="../../docbook-xsl/html/highlight.xsl" />

	<!-- Enable syntax highlighting of code elements -->
	<xsl:import href="highlighting.xsl" />

	<!-- Import customization layers -->
	<xsl:import href="html-custom.xsl" />

	<!-- Add DOCTYPE to the top of HTML output to make it Internet Explorer 
		compatible (no quirks mode) -->
	<xsl:output doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
		doctype-system="http://www.w3.org/TR/html4/loose.dtd" />

	<!-- Changes the navigation text from "Home" to "Table of Contents" -->
	<xsl:param name="local.l10n.xml" select="document('')" />
	<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
		<l:l10n language="en">
			<l:gentext key="nav-home" text="Table of Contents" />
		</l:l10n>
	</l:i18n>

	<!-- Adds destination text for the "Prev" and "Next" links Overrides the 
		header.navigation template in chunk-common.xml -->
	<xsl:template name="header.navigation">
		<xsl:param name="prev" select="/foo" />
		<xsl:param name="next" select="/foo" />
		<xsl:param name="nav.context" />

		<xsl:variable name="home" select="/*[1]" />
		<xsl:variable name="up" select="parent::*" />

		<xsl:variable name="row1" select="$navig.showtitles != 0" />
		<xsl:variable name="row2"
			select="count($prev) &gt; 0
                                    or (count($up) &gt; 0 
                                        and generate-id($up) != generate-id($home)
                                        and $navig.showtitles != 0)
                                    or count($next) &gt; 0" />
		<xsl:variable name="row3" select="1" />

		<xsl:if
			test="$suppress.navigation = '0' and $suppress.header.navigation = '0'">
			<div class="navheader">
				<xsl:if test="$row1 or $row2">
					<table width="100%" summary="Navigation header">
						<xsl:if test="$row1">
							<tr>
								<th colspan="3" align="center">
									<xsl:choose>
										<xsl:when test="$home != . or $nav.context = 'toc'">
											<a accesskey="h">
												<xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object"
													select="$home" />
                        </xsl:call-template>
                      </xsl:attribute>
												<xsl:call-template name="navig.content">
													<xsl:with-param name="direction" select="'home'" />
												</xsl:call-template>
											</a>
											<xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
												<xsl:text>&#160;|&#160;</xsl:text>
											</xsl:if>
										</xsl:when>
										<xsl:otherwise>
											&#160;
										</xsl:otherwise>
									</xsl:choose>
								</th>
							</tr>
						</xsl:if>

						<xsl:if test="$row2">
							<tr>
								<td width="20%" align="left">
									<xsl:if test="count($prev)>0">
										<a accesskey="p">
											<xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object"
												select="$prev" />
                      </xsl:call-template>
                    </xsl:attribute>
											<xsl:call-template name="navig.content">
												<xsl:with-param name="direction" select="'prev'" />
											</xsl:call-template>
										</a>
									</xsl:if>
									<xsl:text>&#160;</xsl:text>
								</td>
								<th width="60%" align="center">
									<xsl:choose>
										<xsl:when
											test="count($up) > 0
                                  and generate-id($up) != generate-id($home)
                                  and $navig.showtitles != 0">
											<a accesskey="u">
												<xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object"
													select="$up" />
                        </xsl:call-template>
                      </xsl:attribute>
												<xsl:apply-templates select="$up"
													mode="object.title.markup" />
											</a>
										</xsl:when>
										<xsl:otherwise>
											&#160;
										</xsl:otherwise>
									</xsl:choose>
								</th>
								<td width="20%" align="right">
									<xsl:text>&#160;</xsl:text>
									<xsl:if test="count($next)>0">
										<a accesskey="n">
											<xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object"
												select="$next" />
                      </xsl:call-template>
                    </xsl:attribute>
											<xsl:call-template name="navig.content">
												<xsl:with-param name="direction" select="'next'" />
											</xsl:call-template>
										</a>
									</xsl:if>
								</td>
							</tr>
						</xsl:if>

						<xsl:if test="$row3">
							<tr>
								<td width="20%" align="left">
									<xsl:if test="$navig.showtitles != 0">
										<a>
											<xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object"
												select="$prev" />
                      </xsl:call-template>
                    </xsl:attribute>
											<xsl:apply-templates select="$prev"
												mode="object.title.markup" />
										</a>
									</xsl:if>
									<xsl:text>&#160;</xsl:text>
								</td>
								<th width="60%" align="center">
									<xsl:apply-templates select="."
										mode="object.title.markup" />
								</th>
								<td width="20%" align="right">
									<xsl:if test="$navig.showtitles != 0">
										<a>
											<xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object"
												select="$next" />
                      </xsl:call-template>
                    </xsl:attribute>
											<xsl:apply-templates select="$next"
												mode="object.title.markup" />
										</a>
									</xsl:if>
									<xsl:text>&#160;</xsl:text>
								</td>
							</tr>
						</xsl:if>

					</table>
				</xsl:if>
				<xsl:if test="$header.rule != 0">
					<hr />
				</xsl:if>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template name="footer.navigation">
		<xsl:param name="prev" select="/foo" />
		<xsl:param name="next" select="/foo" />
		<xsl:param name="nav.context" />

		<xsl:variable name="home" select="/*[1]" />
		<xsl:variable name="up" select="parent::*" />

		<xsl:variable name="row1"
			select="count($prev) &gt; 0
                                    or count($up) &gt; 0
                                    or count($next) &gt; 0" />

		<xsl:variable name="row2"
			select="($prev and $navig.showtitles != 0)
                                    or (generate-id($home) != generate-id(.)
                                        or $nav.context = 'toc')
                                    or ($chunk.tocs.and.lots != 0
                                        and $nav.context != 'toc')
                                    or ($next and $navig.showtitles != 0)" />

		<xsl:if
			test="$suppress.navigation = '0' and $suppress.footer.navigation = '0'">
			<div class="navfooter">
				<xsl:if test="$footer.rule != 0">
					<hr />
				</xsl:if>

				<xsl:if test="$row1 or $row2">
					<table width="100%" summary="Navigation footer">
						<xsl:if test="$row1">
							<tr>
								<td width="40%" align="left">
									<xsl:if test="count($prev)>0">
										<a accesskey="p">
											<xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object"
												select="$prev" />
                      </xsl:call-template>
                    </xsl:attribute>
											<xsl:call-template name="navig.content">
												<xsl:with-param name="direction" select="'prev'" />
											</xsl:call-template>
										</a>
									</xsl:if>
									<xsl:text>&#160;</xsl:text>
								</td>
								<td width="20%" align="center">
									<xsl:choose>
										<xsl:when
											test="count($up)&gt;0
                                  and generate-id($up) != generate-id($home)">
											<a accesskey="u">
												<xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object"
													select="$up" />
                        </xsl:call-template>
                      </xsl:attribute>
												<xsl:call-template name="navig.content">
													<xsl:with-param name="direction" select="'up'" />
												</xsl:call-template>
											</a>
										</xsl:when>
										<xsl:otherwise>
											&#160;
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td width="40%" align="right">
									<xsl:text>&#160;</xsl:text>
									<xsl:if test="count($next)>0">
										<a accesskey="n">
											<xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object"
												select="$next" />
                      </xsl:call-template>
                    </xsl:attribute>
											<xsl:call-template name="navig.content">
												<xsl:with-param name="direction" select="'next'" />
											</xsl:call-template>
										</a>
									</xsl:if>
								</td>
							</tr>
						</xsl:if>

						<xsl:if test="$row2">
							<tr>
								<td width="40%" align="left" valign="top">
									<xsl:if test="$navig.showtitles != 0">
										<xsl:apply-templates select="$prev"
											mode="object.title.markup" />
									</xsl:if>
									<xsl:text>&#160;</xsl:text>
								</td>
								<td width="20%" align="center">
									<xsl:choose>
										<xsl:when test="$home != . or $nav.context = 'toc'">
											<a accesskey="h">
												<xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object"
													select="$home" />
                        </xsl:call-template>
                      </xsl:attribute>
												<xsl:call-template name="navig.content">
													<xsl:with-param name="direction" select="'home'" />
												</xsl:call-template>
											</a>
											<xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
												<xsl:text>&#160;|&#160;</xsl:text>
											</xsl:if>
										</xsl:when>
										<xsl:otherwise>
											&#160;
										</xsl:otherwise>
									</xsl:choose>

									<xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
										<a accesskey="t">
											<xsl:attribute name="href">
                      <xsl:apply-templates select="/*[1]"
												mode="recursive-chunk-filename">
                        <xsl:with-param name="recursive"
												select="true()" />
                      </xsl:apply-templates>
                      <xsl:text>-toc</xsl:text>
                      <xsl:value-of select="$html.ext" />
                    </xsl:attribute>
											<xsl:call-template name="gentext">
												<xsl:with-param name="key" select="'nav-toc'" />
											</xsl:call-template>
										</a>
									</xsl:if>
								</td>
								<td width="40%" align="right" valign="top">
									<xsl:text>&#160;</xsl:text>
									<xsl:if test="$navig.showtitles != 0">
										<xsl:apply-templates select="$next"
											mode="object.title.markup" />
									</xsl:if>
								</td>
							</tr>
						</xsl:if>
					</table>
				</xsl:if>
			</div>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
