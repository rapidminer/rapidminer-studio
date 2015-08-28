<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
 xmlns:rmdoc="com.rapidminer.gui.OperatorDocToHtmlConverter">
<!-- <xsl:script implements-prefix="pref" language="java" src="java:com.rapid_i.test.OperatorDocToHtmlConverter" / -->
	<xsl:template match="/">
		<html>
			<head>
				<!-- <style type="text/css">
					h2
					{font-family: Arial;}
					h3
					{font-family: Arial; color: #3399FF;}
					.HeadIcon
					{height: 40px; width: 40px}
					p
					{padding: 0px 20px 1px 20px; font-family: Arial; /*font-size: 75%/*}
					ul
					{font-family: Arial; /*font-size: 75%*/}
					td
					{font-family: Arial/*; font-size: 75%*/; vertical-align: top}
				</style> -->
			</head>
			<body>	
			  <xsl:apply-templates />		
			</body>
		</html>
	</xsl:template>
	<xsl:template match="title">
		
	</xsl:template>
	<xsl:template match="operator">
		<xsl:variable name="operatorKey">
			<xsl:value-of select="@key" />
		</xsl:variable>
			<table>
				<tr>
					<td>
						<img>
							<xsl:attribute name="src"><xsl:value-of select="rmdoc:getIconNameForOperator($operatorKey)" /></xsl:attribute>
							<xsl:attribute name="class">HeadIcon</xsl:attribute>
						</img>
					
					</td>
					<td valign="middle" align="center">
						<h2><xsl:value-of select="title" />
							<xsl:if test="boolean(rmdoc:getPluginNameForOperator($operatorKey))">
  								<small style="font-weight:normal;font-size:70%;color:#5F5F5F;"> (<xsl:value-of select="rmdoc:getPluginNameForOperator($operatorKey)" />)</small>
							</xsl:if>
						</h2>
					</td>
				</tr>
			</table>
			<hr noshade="true" />
			<xsl:apply-templates />
		</xsl:template>
	<xsl:template match="synopsis">
		<h3>Synopsis</h3>
		<p>
			<xsl:apply-templates />
		</p>
	</xsl:template>
	<xsl:template match="text">
		<h3>Description</h3>
		<xsl:apply-templates />	
	</xsl:template>
	<xsl:template match="paragraph">
		<p>
			<xsl:apply-templates />
		</p>
	</xsl:template>
	<xsl:template match="em">
		<em><xsl:value-of select="." /></em>
	</xsl:template>
	
	<xsl:template match="code">
		<code><xsl:value-of select="." /></code>
	</xsl:template>
	
	<xsl:template match="differentiation">
		
			<h3>Differentiation</h3>
			<xsl:apply-templates />
		
	</xsl:template>

	<xsl:template match="relatedDocuments">
		<xsl:if test="count(/relatedDocument)>0">
			<h3>Related Documents</h3>
			<xsl:apply-templates />
		</xsl:if>
	</xsl:template>

	<xsl:template match="relatedDocument">
		<xsl:variable name="relatedOpKey">
			<xsl:value-of select="@key" />
		</xsl:variable>

		<h4 style="vertical-align:middle;">
			<span style="vertical-align:middle;">			
			<img>
				<xsl:attribute name="src"><xsl:value-of select="rmdoc:getIconNameForOperatorSmall($relatedOpKey)" /></xsl:attribute>
				<xsl:attribute name="class">HeadIcon</xsl:attribute>
				<xsl:attribute name="style">vertical-align:middle;margin-right:8px;</xsl:attribute>											
			</img>					
			<!-- a style="vertical-align:middle;" href="opdoc:{$relatedOpKey}.html" -->
			<span style="vertical-align:middle;padding-bottom:5px;">
			<xsl:value-of select="rmdoc:getOperatorNameForKey($relatedOpKey)" />
			</span>			
			<!-- /a -->
			</span>
		</h4>
		<div style="margin-bottom:10px">
			<xsl:apply-templates />
		</div>
	</xsl:template>
	
	<xsl:template match="inputPorts">
		<h3>Input</h3>
	
		<table border="0" cellspacing="7">
			<xsl:for-each select="port">
				<tr>
					<td>
						<table>
							<tr>
								<td class="lilIcon">
									<img><xsl:attribute name="src"><xsl:value-of select="rmdoc:getIconNameForType(@type)"/></xsl:attribute>
									<xsl:attribute name="class">typeIcon</xsl:attribute></img>
								</td>
								<td> 
									<b>
							 			<xsl:value-of select="rmdoc:insertBlanks(@name)" />
									</b>
									<i>
										<xsl:value-of select="rmdoc:getTypeNameForType(@type)"/>
									</i>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:if test="@type = 'through'">
							<p>It is not compulsory to connect any object with this port. Any object connected at this port is delivered without any modifications to the output port. This operator can have multiple inputs. When one input is connected, another <em>through</em> input port becomes available which is ready to accept another input (if any). The order of inputs remains the same. The object supplied at the first <em>through</em> input port of the operator is available at the first <em>through</em> output port.</p>
						</xsl:if>
						<xsl:value-of select="."/>
					</td>
				</tr>
			</xsl:for-each>
		</table>
		
	</xsl:template>
	
	<xsl:template match="outputPorts">
		<h3>Output</h3>
		
		<table border="0" cellspacing="7">
			<xsl:for-each select="port">
				<tr>
					<td>
						<table>
							<tr>
								<td class="lilIcon">
									<img><xsl:attribute name="src"><xsl:value-of select="rmdoc:getIconNameForType(@type)"/></xsl:attribute>
									<xsl:attribute name="class">typeIcon</xsl:attribute></img>
								</td>
								<td> 
									<b>
							 			<xsl:value-of select="rmdoc:insertBlanks(@name)" />
									</b>
									<i>
										<xsl:value-of select="rmdoc:getTypeNameForType(@type)"/>
									</i>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:if test="@type = 'through'">
							<p>Objects that were given as input are passed without changing to the output through this port. It is not compulsory to attach this port to any other port, the macro value is set even if this port is left without connections. The operator can have multiple outputs. When one output is connected, another <em>through</em> output port becomes available which is ready to deliver another output (if any). The order of outputs remains the same. The object delivered at the first <em>through</em> input port of the operator is delivered at the first <em>through</em> output port.</p>
						</xsl:if>
						<xsl:value-of select="."/>
					</td>
				</tr>
			</xsl:for-each>
		</table>
		
	</xsl:template>
	<xsl:template match="parameters">
		<h3>Parameters</h3>
		<table border="0" cellspacing="7">
			<xsl:for-each select="parameter">
				<tr>
					<td width="75">
						<b>
							<xsl:value-of select="rmdoc:insertBlanks(@key)" />
						</b>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:apply-templates /> 
						<b> Range: </b> <i> <xsl:value-of select="@type" /> </i>
					</td>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>
	
	<xsl:template match="values">
		<ul>
			<xsl:for-each select="value">
				<li>
					<b><xsl:value-of select="@value" /></b>: <xsl:value-of select="."/>
				</li>
			</xsl:for-each>
		</ul>
	</xsl:template>
	
		<xsl:template match="ul">
		<ul>
			
	 		<xsl:for-each select="li">
				<li>
					<xsl:value-of select="." />
				</li>
			</xsl:for-each>
		</ul>
	</xsl:template>

	<xsl:template match="tutorialProcesses">
		<h3>Tutorial Process</h3>
			<xsl:for-each select="tutorialProcess">
				<p>
					<a>
						<xsl:attribute name="href">tutorial:<xsl:value-of select="position()" /></xsl:attribute>
						<xsl:value-of select="@title" />
					</a>
				</p>
				<xsl:for-each select="description/paragraph">
					<xsl:choose>
						<xsl:when test="ul">
							<xsl:for-each select="ul">
						 		<ul>			
	 								<xsl:for-each select="li">
										<li>
											<xsl:value-of select="." />
										</li>
									</xsl:for-each>
								</ul>
					
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<p>
								<xsl:value-of select="." />
							</p>
						</xsl:otherwise>
					</xsl:choose>					
				</xsl:for-each>
			</xsl:for-each>
	</xsl:template>



</xsl:stylesheet>