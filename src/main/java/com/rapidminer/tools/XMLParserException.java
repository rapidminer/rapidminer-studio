/**
 * Copyright (C) 2001-2018 RapidMiner GmbH
 */
package com.rapidminer.tools;

import java.io.IOException;


/**
 * Marker Exception to use special Exception handling if required
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class XMLParserException extends IOException {
	public XMLParserException(Throwable e) {
		super(e);
	}
}
