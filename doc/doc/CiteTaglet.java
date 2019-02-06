/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A taglet with name &quot;@rapidminer.cite&quot; can be used in the Javadoc comments of an operator to produce a reference
 * to literature. Example: &quot;@rapidminer.cite Mierswa/etal/2003a&quot;. This will include a LaTeX cite command to your
 * document.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class CiteTaglet implements TexTaglet {

	private static final String NAME = "rapidminer.cite";

	public String getName() {
		return NAME;
	}

	public boolean inField() {
		return true;
	}

	public boolean inConstructor() {
		return true;
	}

	public boolean inMethod() {
		return true;
	}

	public boolean inOverview() {
		return true;
	}

	public boolean inPackage() {
		return true;
	}

	public boolean inType() {
		return true;
	}

	public boolean isInlineTag() {
		return true;
	}

	public static void register(Map<String, Taglet> tagletMap) {
		CiteTaglet tag = new CiteTaglet();
		Taglet t = tagletMap.get(tag.getName());
		if (t != null) {
			tagletMap.remove(tag.getName());
		}
		tagletMap.put(tag.getName(), tag);
	}

	public String toString(Tag tag) {
		return "[" + tag.text() + "]";
	}

	public String toString(Tag[] tags) {
		return null;
	}

	public String toTex(Tag tag) {
		return "\\cite{" + tag.text() + "}";
	}

	public String toTex(Tag[] tag) {
		return null;
	}
}
