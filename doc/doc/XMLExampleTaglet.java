/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.rapidminer.tools.Tools;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;


/**
 * A taglet with name &quot;@rapidminer.xmlinput&quot; can be used in the Javadoc comments of an operator to include an XML
 * file into the documentation. Example: &quot;@rapidminer.xmlinput filename|label|caption&quot;. This may be useful to
 * provide the XML code for an operator's usage.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class XMLExampleTaglet implements TexTaglet {

	private static final String NAME = "rapidminer.xmlinput";

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
		XMLExampleTaglet tag = new XMLExampleTaglet();
		Taglet t = tagletMap.get(tag.getName());
		if (t != null) {
			tagletMap.remove(tag.getName());
		}
		tagletMap.put(tag.getName(), tag);
	}

	private String[] split(Tag tag) {
		String[] result = tag.text().split("\\|");
		if (result.length != 3) {
			System.err.println("Usage: {@" + getName() + " filename|label|caption} (was: " + tag.text() + ") (" + tag.position() + ")");
			return null;
		}
		return result;
	}

	private File resolve(String file, SourcePosition source) {
		return new File(source.file().getParentFile(), file);
	}

	public String toString(Tag tag) {
		String[] splitted = split(tag);
		if (splitted == null)
			return "";
		File file = resolve(splitted[0], tag.position());
		String contents = null;
		if (file.exists()) {
			try {
				contents = Tools.readTextFile(file);
				contents = contents.replaceAll("<", "&lt;");
				contents = contents.replaceAll(">", "&gt;");
			} catch (IOException e) {
				contents = "Cannot read file '" + file + "': " + e;
				System.err.println(tag.position() + ": cannot read file '" + file + "'!");
			}
		} else {
			contents = "File '" + file + "' does not exist!";
			System.err.println(tag.position() + ": File '" + file + "' does not exist!");
		}
		return "<pre>" + contents + "</pre><br><center><i>Figure:</i> " + splitted[2] + "</center>";
	}

	public String toString(Tag[] tags) {
		return null;
	}

	public String toTex(Tag tag) {
		String[] splitted = split(tag);
		if (splitted == null)
			return "";
		File file = resolve(splitted[0], tag.position());
		if (!file.exists()) {
			System.err.println(tag.position() + ": File '" + file + "' does not exist!");
			return "";
		} else {
			return "\\examplefile{" + file.getAbsolutePath() + "}{" + splitted[1] + "}{" + splitted[2] + "}";
		}
	}

	public String toTex(Tag[] tag) {
		return null;
	}
}
