/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A taglet with name &quot;@rapidminer.ref&quot; can be used in the Javadoc comments of an operator to produce textual
 * references. Example: &quot;@rapidminer.ref figure1|A figure for this&quot;. This will include a LaTeX reference to your
 * documentation.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class RefTaglet implements TexTaglet {

	private static final String NAME = "rapidminer.ref";

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
		RefTaglet tag = new RefTaglet();
		Taglet t = tagletMap.get(tag.getName());
		if (t != null) {
			tagletMap.remove(tag.getName());
		}
		tagletMap.put(tag.getName(), tag);
	}

	private String[] split(Tag tag) {
		String[] splitted = tag.text().split("\\|");
		if (splitted.length != 2) {
			System.err.println("Usage: {@" + getName() + " latexref|html_human_readable_ref} (" + tag.position() + ")");
			return new String[] { tag.text(), tag.text() };
		} else {
			return splitted;
		}
	}

	public String toString(Tag tag) {
		return split(tag)[1];
	}

	public String toString(Tag[] tags) {
		return null;
	}

	public String toTex(Tag tag) {
		return "\\ref{" + split(tag)[0] + "}";
	}

	public String toTex(Tag[] tag) {
		return null;
	}
}
