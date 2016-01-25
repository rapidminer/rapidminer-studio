<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
 xmlns:rmdoc="com.rapidminer.gui.OperatorDocToHtmlConverter">
<xsl:variable name="operatorKey" select="//operator/@key"/>
<!-- <xsl:script implements-prefix="pref" language="java" src="java:com.rapid_i.test.OperatorDocToHtmlConverter" / -->
	<xsl:template match="/">
		<html>
			<head/>
			<body>
			  <xsl:apply-templates />
			</body>
		</html>
	</xsl:template>
	<xsl:template match="title">
		
	</xsl:template>
	<xsl:template match="operator">
			<table>
				<tr>
					<td>
						<img>
							<xsl:attribute name="src"><xsl:value-of select="rmdoc:getIconNameForOperator($operatorKey)" /></xsl:attribute>
							<xsl:attribute name="class">HeadIcon</xsl:attribute>
						</img>
					
					</td>
					<td valign="middle" align="left">
						<h2><xsl:value-of select="title" />
							<xsl:if test="boolean(rmdoc:getPluginNameForOperator($operatorKey))">
  								<span class="packageName"><br/><xsl:value-of select="rmdoc:getPluginNameForOperator($operatorKey)" /></span>
							</xsl:if>
						</h2>
					</td>
				</tr>
			</table>
			<div style="border-top: 1px solid #bbbbbb">
			<xsl:apply-templates />
			</div>
		</xsl:template>
	<xsl:template match="synopsis">
		<h4>Synopsis</h4>
		<p>
			<xsl:apply-templates />
		<xsl:if test="//tutorialProcess">
			<p class="tutorialProcessLink"><a href="#tutorialProcesses">Jump to Tutorial Process</a></p>
		</xsl:if>
		</p><br/>
	</xsl:template>
	<xsl:template match="text">
		<h4>Description</h4>
		<xsl:apply-templates />	
		<br/>
	</xsl:template>
	<xsl:template match="paragraph">
		<p>
			<xsl:apply-templates />
		</p>
		<br/>
	</xsl:template>
	<xsl:template match="em">
		<em><xsl:value-of select="." /></em>
	</xsl:template>
	
	<xsl:template match="code">
		<code><xsl:value-of select="." /></code>
	</xsl:template>
	
	<xsl:template match="differentiation">
		
			<h4>Differentiation</h4>
			<xsl:apply-templates />
			<br/>
	</xsl:template>

	<xsl:template match="relatedDocuments">
		<xsl:if test="count(/relatedDocument)>0">
			<h4>Related Documents</h4>
			<xsl:apply-templates />
		</xsl:if>
		<br/>
	</xsl:template>

	<xsl:template match="relatedDocument">
		<xsl:variable name="relatedOpKey">
			<xsl:value-of select="@key" />
		</xsl:variable>

		<h5 style="vertical-align:middle;">
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
		</h5>
		<div style="margin-bottom:10px">
		<p><xsl:apply-templates /></p>
			
		</div>
	</xsl:template>
	
	<xsl:template match="inputPorts">
		<h4>Input</h4>
	
		<table border="0" cellspacing="0">
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
		<br/>
	</xsl:template>
	
	<xsl:template match="outputPorts">
		<h4>Output</h4>
		
		<table border="0" cellspacing="0">
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
		<br/>
	</xsl:template>
	<xsl:template match="parameters">
		<h4>Parameters</h4>
		<table border="0" cellspacing="0">
			<xsl:for-each select="parameter">
				<tr>
					<td width="75">
						<b>
							<xsl:value-of select="rmdoc:insertBlanks(@key)" />
						</b>
						<xsl:choose>
							<xsl:when test="@optional != 'false'">
								<!-- Do nothing -->
							</xsl:when>
							<xsl:when test="@optional != 'true'">
								(optional)
							</xsl:when>
							<xsl:otherwise>
								<xsl:if test="rmdoc:isParameterOptional($operatorKey,@key) = 'true'">
									(optional)
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:apply-templates />
						<span class="parameterDetails">
						
						<xsl:variable name="parameterType">
							<xsl:value-of select="rmdoc:getParameterType($operatorKey,@key)"></xsl:value-of>
						</xsl:variable>
						<xsl:choose>
							<xsl:when test="@type != ''">
								<br/><b>Type: </b> <i><xsl:value-of select="@type"></xsl:value-of></i>
							</xsl:when>
							<xsl:when test="$parameterType != ''">
								<br/><b>Type: </b> <i><xsl:value-of select="$parameterType" /></i>
							</xsl:when>
						</xsl:choose>
						
						<xsl:variable name="parameterRange">
							<xsl:value-of select="rmdoc:getParameterRange($operatorKey,@key)"></xsl:value-of>
						</xsl:variable>
						<xsl:choose>
							<xsl:when test="@type = 'real' or @type = 'integer' or @type = 'Long' or @type = 'selection'">
								<xsl:choose>
									<xsl:when test="@range != ''">
										<br/><b>Range: </b> <i><xsl:value-of select="@range"></xsl:value-of></i>
									</xsl:when>
									<xsl:when test="$parameterRange != ''">
										<br/><b>Range: </b> <i><xsl:value-of select="$parameterRange" /></i>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
							<xsl:when test="$parameterType = 'real' or $parameterType = 'integer' or $parameterType = 'Long' or $parameterType = 'selection'">
								<xsl:choose>
									<xsl:when test="@range != ''">
										<br/><b>Range: </b> <i><xsl:value-of select="@range"></xsl:value-of></i>
									</xsl:when>
									<xsl:when test="$parameterRange != ''">
										<br/><b>Range: </b> <i><xsl:value-of select="$parameterRange" /></i>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
						</xsl:choose>
						
						<xsl:variable name="parameterDefault">
							<xsl:value-of select="rmdoc:getParameterDefault($operatorKey,@key)"></xsl:value-of>
						</xsl:variable>
						<xsl:choose>
							<xsl:when test="@default != ''">
								<br/><b>Default: </b> <i><xsl:value-of select="@default"></xsl:value-of></i>
							</xsl:when>
							<xsl:when test="$parameterDefault != ''">
								<br/><b>Default: </b> <i><xsl:value-of select="$parameterDefault" /></i>
							</xsl:when>
						</xsl:choose>
						
						</span>
					</td>
				</tr>
			</xsl:for-each>
		</table>
		<br/>
	</xsl:template>
	
	<xsl:template match="values">
		<table>
			<xsl:for-each select="value">
				<tr>
					<td valign="top" style="font-family: Ionicons; font-style: normal;">&#xf10a;</td>
					<td><b><xsl:value-of select="@value" /></b>: <xsl:value-of select="."/></td>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>
	
	<xsl:template match="ul">
		<table>
	 		<xsl:for-each select="li">
		 		<tr>
		 		<td valign="top" style="font-family: Ionicons; font-style: normal;">&#xf10a;</td>
		 		<td><xsl:apply-templates/></td>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>

	<xsl:template match="tutorialProcesses">
		<h4> <a name="tutorialProcesses">Tutorial Process</a></h4>
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
						 		<table style="padding-left:10px;">			
	 								<xsl:for-each select="li">
										<tr>
											<td valign="top" style="font-family: Ionicons; font-style: normal;">&#xf10a;</td>
			 								<td><xsl:value-of select="." /></td>
										</tr>
									</xsl:for-each>
								</table>
					
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<p>
								<xsl:value-of select="." />
							</p>
						</xsl:otherwise>
					</xsl:choose>					
				</xsl:for-each>
				<br/>
			</xsl:for-each>
	</xsl:template>



</xsl:stylesheet>