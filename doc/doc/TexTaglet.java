/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * Creates the LaTeX code from a html taglet.
 * 
 * @author Simon Fischer
 */
public interface TexTaglet extends Taglet {

	public String toTex(Tag tag);

	public String toTex(Tag[] tag);

}
