/**
 * Copyright (C) 2001-2018 RapidMiner GmbH
 */
package com.rapidminer.io.process;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * @author Andreas Timm
 * @since 8.1
 */
public class XMLToolsTest {

	@Test(expected = SAXParseException.class)
	public void loadCorruptXml() throws IOException, SAXException {
		XMLTools.createDocumentBuilder().parse(XMLToolsTest.class.getResourceAsStream("/com/rapidminer/io/process/XXE_corrupted.xml"));
	}

	@Test
	public void loadSomeProcessXml() throws IOException, SAXException {
		Document document = XMLTools.createDocumentBuilder().parse(XMLToolsTest.class.getResourceAsStream("/com/rapidminer/io/process/some_process.xml"));
		Assert.assertNotNull(document);
	}
}
