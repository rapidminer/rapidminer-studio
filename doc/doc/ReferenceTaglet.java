/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.kobjects.jdbc.TableManager;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A taglet with name &quot;@rapidminer.reference&quot; can be used in the Javadoc comments of an operator to produce a
 * reference to literature including the bibtex entry. Unfortunately this can only be used in the Artificial
 * Intelligence Unit of the University of Dortmund.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class ReferenceTaglet implements Taglet {

	//private static final String[] DOCUMENT_EXTENSIONS = { "ps", "ps.gz", "pdf", "pdf.gz", "ppt" };

	private static final String NAME = "rapidminer.reference";

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

	private static Method getResultSetMethod;

	private static File rapidMinerHome;

	/**
	 * Find TableManager.getResultSet() method by reflection, since as far as we know it is not possible to include
	 * kdb.jar in the classpath.
	 */
	static {
		String rapidMinerHomeName = System.getProperty("rapidminer.home"); // TODO [property]
		if (rapidMinerHomeName == null) {
			File buildDir = new File(ReferenceTaglet.class.getResource(".."+File.separator+".." + File.separator + "..").getFile());
			rapidMinerHome = buildDir.getParentFile();
			System.err.println("rapidminer.home is not set! Assuming " + rapidMinerHome);
		} else {
			rapidMinerHome = new File(rapidMinerHomeName);
		}

		if (rapidMinerHome != null) {
			try {
				URL url = new URL("file", null, new File(rapidMinerHome, "lib" + File.separator + "kdb.jar").getAbsolutePath());
				ClassLoader classLoader = new URLClassLoader(new URL[] { url });
				Class<?> tableManagerClass = classLoader.loadClass("org.kobjects.jdbc.TableManager");
				getResultSetMethod = tableManagerClass.getMethod("getResultSet", new Class[] { String.class, int.class });
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.err.println("Cannot find class org.kobjects.jdbc.TableManager");
			} catch (NoSuchMethodException e) {
				System.err.println("Cannot find method org.kobjects.jdbc.TableManager.getResultSet(String,int)");
			}
		}
	}

	public static void register(Map<String, Taglet> tagletMap) {
		ReferenceTaglet tag = new ReferenceTaglet();
		Taglet t = tagletMap.get(tag.getName());
		if (t != null) {
			tagletMap.remove(tag.getName());
		}
		tagletMap.put(tag.getName(), tag);
	}

	public String toString(Tag tag) {
		return toString(new Tag[] { tag });
	}

	public String toString(Tag[] tags) {
		if (tags.length == 0) {
			return null;
		}
		String result = "<dt><b>Bibliography:</b></dt><dd>";
		result += "<ul>";
		for (int i = 0; i < tags.length; i++) {
			String key = tags[i].text().trim();
			String entry = getBibEntry(key);
			result += "<li>" + ((entry != null) ? entry : key) + "</li>";
		}
		result += "</ul></dd>";
		return result;
	}

	private static String getBibEntry(String key) {
		if (getResultSetMethod == null)
			return null;

		File bibFile = new File(rapidMinerHome, "tutorial" + File.separator + "RapidMinerTutorial.bib");
		if (!bibFile.exists()) {
			System.err.println("rapidminer.home is not set! Cannot find RapidMinerTutorial.bib");
			return null;
		}
		ResultSet literatur = null;
		try {
			literatur = (ResultSet) getResultSetMethod.invoke(null, new Object[] { "bibtex:" + bibFile, Integer.valueOf(TableManager.READ) });
		} catch (IllegalAccessException e) {
			System.err.println("Cannot access TableManager.getResultSet(): " + e);
			getResultSetMethod = null;
			return null;
		} catch (InvocationTargetException e) {
			System.err.println("Exception in TableManager.getResultSet(): " + e.getCause());
		}

		if (literatur == null)
			return null;

		try {
			while (literatur.next()) {
				String bibkey = literatur.getString("bibkey");
				if (bibkey == null)
					continue;
				if (bibkey.equals(key)) {
					String result = "[" + key + "] ";
					result += escape(literatur.getString("author")) + ": " + "<i>" + escape(literatur.getString("title")) + "</i> ";
					String in = literatur.getString("booktitle");
					if (in != null) {
						result += "In " + escape(in) + " ";
					}
					result += "(" + literatur.getString("year") + ")";
					result += "</li>";
					return result;
				}
			}
			System.err.println("Bibkey not found: " + key);
			return null;
		} catch (SQLException e) {
			System.err.println("SQLException occured: " + e.getMessage());
			return null;
		}
	}

	private static String escape(String str) {
		if (str == null)
			return null;
		String escaped = str;
		escaped = escaped.replaceAll("<", "&lt;");
		escaped = escaped.replaceAll(">", "&gt;");
		escaped = escaped.replaceAll("\\{", "");
		escaped = escaped.replaceAll("\\}", "");
		return escaped;
	}
}
