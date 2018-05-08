<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html"/>

	<xsl:template match="/">
		<html>
		<style>
			ul, ol {padding:10px;padding-top:5px;margin:0px;margin-left:12px;font-family:'Open Sans';font-size:13;}
			.text {margin:10px;margin-top:0px;margin-bottom:20px;font-family:'Open Sans';font-size:13;}
			.info {padding:10px;padding-top:5px;margin-bottom:20px;font-family:'Open Sans';font-size:13;}
			.tasks, .questions {margin-bottom:20px;font-family:'Open Sans';font-size:13;}
			.stepName {margin:10px;margin-bottom:20px;margin-top:0px;color:#555555;padding-bottom:5px;border-style:solid;border-width: 0 0 1 0;border-color:#BBBBBB;font-family:'Open Sans Bold';font-size:14;text-align:center;}
			.stepNum {margin-top:10px;color:#555555;font-family:'Open Sans';font-size:12;text-align:center;}
		</style>
		<body>
		<div class="stepNum">
    		<xsl:value-of select="step/@index"/>/<xsl:value-of select="step/@total"/>
    	</div>
    	<div class="stepName">
    		<xsl:value-of select="step/@name"/>
    	</div>
		<xsl:apply-templates/>
    	</body></html>
	</xsl:template>

	<xsl:template match="text">
		<div class="text">
			<xsl:apply-templates/>
		</div>
	</xsl:template>

	<xsl:template match="info">
		<div style="background-image: url({@background-image});background-repeat:repeat-x;float:left;text-align:center;">
			<img src="{@image}"></img></div>
		<div class="info" style="background-color:#E1F0FA;">
			<xsl:apply-templates/>
		</div>
	</xsl:template>

	<xsl:template match="tasks|questions">
		<div style="background-image: url({@background-image});background-repeat:repeat-x;float:left;text-align:center;">
			<img src="{@image}"></img></div>
		<xsl:choose>
			<xsl:when test="question">
				<div class="questions" style="background-color:#E0FCEF;">
					<ul>
						<xsl:apply-templates/>
					</ul>   
    			</div>
			</xsl:when>
        	<xsl:otherwise>
				<div class="tasks" style="background-color:#F7EDE6;">
					<ol>
						<xsl:apply-templates/>
					</ol>   
				</div> 
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="task|question|point">
		<li><xsl:apply-templates/></li>
	</xsl:template>

	<xsl:template match="ui|op|param|folder|file">
		<b><xsl:apply-templates/></b>
	</xsl:template>
	
	<xsl:template match="emph|value">
		<i><xsl:apply-templates/></i>
	</xsl:template>
	
	<xsl:template match="icon">
		<img src="{@image}"></img>
	</xsl:template>
	
	<xsl:template match="link">
		<a href="{@url}"><xsl:apply-templates/></a>
	</xsl:template>
	
	<xsl:template match="activity">
		<span style="color:#F25800">
			<xsl:apply-templates/>
		</span>
	</xsl:template>


</xsl:stylesheet>