/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package com.rapidminer.doc;

import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * Formats operator documentation in LaTeX style.
 * 
 * @rapidminer.todo Lookup class when link is found and decide which tag to use (op, ioobj, ...)
 * @author Simon Fischer, Ingo Mierswa
 */
public class LatexOperatorDocGenerator extends AbstractOperatorDocGenerator {

	public static final String[][] TAGS = { { "", "" }, // operator
		{ "\\operator{", "}" }, // operator name
		{ "\\group{", "}" }, // group
		{ Tools.getLineSeparator() + "\\begin{parameters}", "\\end{parameters}" }, // parameter list
		{ "", "" }, // parameter item
		{ "\\reqpar[", "]" }, // required parameter
		{ "\\optpar[", "]" }, // optional parameter
		{ "", "" }, // parameter description
		{ "\\paragraph{Short description:} ", "" }, // short description
		{ "\\opdescr ", "" }, // operator description
		{ "\\begin{opin} ", "\\end{opin}" }, // input classes list
		{ "\\begin{opout} ", "\\end{opout}" }, // output classes list
		{ "\\item[", "]" }, // IO class
		{ Tools.getLineSeparator() + "\\paragraph{Inner operators:}", Tools.getLineSeparator() }, // inner operators
		{ Tools.getLineSeparator() + "\\begin{values}", "\\end{values}" }, // value list
		{ "", "" }, // value item
		{ "\\val[", "]" }, // value name
		{ "", "" }, // value description
		{ "\\index{", "}" }, // index entry
		{ "\\par References: ", "" }, // reference section
		{ "\\cite{", "}" }, // reference entry
		{ Tools.getLineSeparator() + "\\paragraph{Further information:}", Tools.getLineSeparator() }, // technical information (external references)
		{ "\\emph{", "}" + Tools.getLineSeparator() }, // deprecation info
		{ Tools.getLineSeparator() + "\\paragraph{Learner capabilities:}", Tools.getLineSeparator() } // learner capabilities
	};

	public String getOpenTag(int tagNo) {
		return TAGS[tagNo][0];
	}

	public String getCloseTag(int tagNo) {
		return TAGS[tagNo][1];
	}

	public String marginIcon(String iconName) {
		String fig = "\\includegraphics{graphics/" + iconName + "}";
		return "\\marginpar[\\flushright" + fig + "]{" + fig + "}";
	}

	public String escape(String toEscape) {
		String escaped = toEscape;
		escaped = escaped.replaceAll("MACRO_START", "\\\\% \\\\{");  // hack for macro definitions
		escaped = escaped.replaceAll("MACRO_END",   "\\\\}");        // hack for macro definitions
		escaped = escaped.replaceAll("_", "\\\\_");
		escaped = escaped.replaceAll("\\$", "\\\\\\$");
		escaped = escaped.replaceAll("\u221E", "\\$\\\\infty\\$");
		escaped = escaped.replaceAll("&auml;", "\\\\\"a");
		escaped = escaped.replaceAll("&ouml;", "\\\\\"o");
		escaped = escaped.replaceAll("&uuml;", "\\\\\"u");
		escaped = escaped.replaceAll("&Auml;", "\\\\\"A");
		escaped = escaped.replaceAll("&Ouml;", "\\\\\"O");
		escaped = escaped.replaceAll("&Uuml;", "\\\\\"U");
		escaped = escaped.replaceAll("&szlig;", "\\\\\"s");
		escaped = escaped.replaceAll("&nbsp;", "\\\\ ");
		escaped = escaped.replaceAll("(\\w)&quot;", "$1''");
		escaped = escaped.replaceAll("&quot;", "``");
		escaped = escaped.replaceAll("#", "\\\\#");
		escaped = escaped.replaceAll("\\[", "\\{\\[\\}");
		escaped = escaped.replaceAll("\\]", "\\{\\]\\}");
		escaped = escaped.replaceAll("RapidMiner", "\\\\RAPIDMINER");
		escaped = escaped.replaceAll("\\\\s", "\\$\\\\backslash\\$s"); // hack for regular expressions (ExampleSource)
		escaped = escaped.replaceAll("\\\\t", "\\$\\\\backslash\\$t"); // hack for regular expressions (ExampleSource)
		escaped = escaped.replaceAll("\\|", "\\$|\\$");
		escaped = escaped.replaceAll("\\^", "");
		return escaped;
	}

	public void beginGroup(String groupName, PrintWriter out) {
		out.println("\\pagebreak[4]");
		if (groupName != null) {
			groupName = groupName.replace(' ', '_');
			out.println("\\input{OpGroup" + groupName + ".tex}");
		} else {
			out.println("\\section{Basic operators}");
		}
	}

	public void endGroup(String groupName, PrintWriter out) {
		out.println("\\vfill");
	}

	public String transformHTMLJavadocComment(String comment, final Class clazz, final String operatorName) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			comment = "<body>" + comment + "</body>";
			final StringBuffer transformed = new StringBuffer();
			final Stack<String> closingTagStack = new Stack<String>();
			parser.parse(new InputSource(new StringReader(comment)), new DefaultHandler() {

				public void characters(char[] ch, int start, int length) throws SAXException {
					transformed.append(ch, start, length);
				}

				public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
					LogService.getGlobal().log("Entity: " + publicId, LogService.STATUS);
					String latex;
					if (systemId.equals("&quot;")) {
						latex = "``";
					} else if (systemId.equals("&auml;")) {
						latex = "\\\"a";
					} else if (systemId.equals("&ouml;")) {
						latex = "\\\"o";
					} else if (systemId.equals("&uuml;")) {
						latex = "\\\"u";
					} else if (systemId.equals("&Auml;")) {
						latex = "\\\"A";
					} else if (systemId.equals("&Ouml;")) {
						latex = "\\\"O";
					} else if (systemId.equals("&Uuml;")) {
						latex = "\\\"U";
					} else if (systemId.equals("&szlig;")) {
						latex = "\\\"s";
					} else if (systemId.equals("&nbsp;")) {
						latex = "\\ ";
					} else {
						LogService.getGlobal().log("Unknown entity: " + systemId, LogService.WARNING);
						latex = systemId;
					}
					return new InputSource(new StringReader(latex));
				}

				public void endElement(String uri, String localName, String qName) {
					transformed.append(closingTagStack.pop());
				}

				public void startElement(String uri, String localName, String qName, Attributes attributes) {
					qName = qName.toLowerCase();
					if (qName.equals("code")) {
						transformed.append("\\java{");
						closingTagStack.push("}");
					} else if (qName.equals("em")) {
						transformed.append("\\emph{");
						closingTagStack.push("}");
					} else if (qName.equals("var")) {
						transformed.append("\\para{");
						closingTagStack.push("}");

					} else if (qName.equals("b")) {
						LogService.getGlobal().log(operatorName + " (" + clazz.getName() + "): physical markup used (b,i, or tt).", LogService.WARNING);
						transformed.append("\\textbf{");
						closingTagStack.push("}");
					} else if (qName.equals("i")) {
						LogService.getGlobal().log(operatorName + " (" + clazz.getName() + "): physical markup used (b,i, or tt).", LogService.WARNING);
						transformed.append("\\textit{");
						closingTagStack.push("}");
					} else if (qName.equals("tt")) {
						LogService.getGlobal().log(operatorName + " (" + clazz.getName() + "): physical markup used (b,i, or tt).", LogService.WARNING);
						transformed.append("\\texttt{");
						closingTagStack.push("}");

					} else if (qName.equals("center")) {
						transformed.append(Tools.getLineSeparator() + "\\begin{center}" + Tools.getLineSeparator());
						closingTagStack.push(Tools.getLineSeparator() + "\\end{center}" + Tools.getLineSeparator());
					} else if (qName.equals("ol")) {
						transformed.append(Tools.getLineSeparator() + "\\begin{enumerate}" + Tools.getLineSeparator());
						closingTagStack.push(Tools.getLineSeparator() + "\\end{enumerate}" + Tools.getLineSeparator());
					} else if (qName.equals("ul")) {
						transformed.append(Tools.getLineSeparator() + "\\begin{itemize}" + Tools.getLineSeparator());
						closingTagStack.push(Tools.getLineSeparator() + "\\end{itemize}" + Tools.getLineSeparator());
					} else if (qName.equals("li")) {
						transformed.append(Tools.getLineSeparator() + "\\item ");
						closingTagStack.push("");
					} else if (qName.equals("dl")) {
						transformed.append(Tools.getLineSeparator() + "\\begin{description}" + Tools.getLineSeparator());
						closingTagStack.push(Tools.getLineSeparator() + "\\end{description}" + Tools.getLineSeparator());
					} else if (qName.equals("dt")) {
						transformed.append(Tools.getLineSeparator() + "\\item[");
						closingTagStack.push("]");
					} else if (qName.equals("dd")) {
						// nothing for dd
						closingTagStack.push("");
					} else if (qName.equals("body")) {
						transformed.append("");
						closingTagStack.push("");
					} else if (qName.equals("sup")) {
						transformed.append("$^{");
						closingTagStack.push("}$");
					} else if (qName.equals("sub")) {
						transformed.append("$_{");
						closingTagStack.push("}$");
					} else if (qName.equals("br")) {
						transformed.append("\\par" + Tools.getLineSeparator());
						closingTagStack.push("");
					} else if (qName.equals("p")) {
						transformed.append("\\par" + Tools.getLineSeparator());
						closingTagStack.push("");
					} else if (qName.equals("a")) {
						closingTagStack.push("\\footnote{\\url{" + attributes.getValue("href") + "}}");
					} else if (qName.equals("h1") || qName.equals("h2") || qName.equals("h3") || qName.equals("h4") || qName.equals("h5")) {
						transformed.append(Tools.getLineSeparator() + "\\paragraph{");
						closingTagStack.push("}");
					} else if (qName.equals("pre")) {
						transformed.append("\\begin{verbatim}");
						closingTagStack.push("\\end{verbatim}");
					} else {
						transformed.append("");
						closingTagStack.push("");
						LogService.getGlobal().log("Unknown tag: " + qName + " (" + operatorName + " (" + clazz.getName() + "))", LogService.WARNING);
					}
				}

			});
			StringBuffer linksReplaced = new StringBuffer();
			Pattern pattern = Pattern.compile("\\{@link (.*?)\\}");
			Matcher matcher = pattern.matcher(transformed);
			while (matcher.find()) {
				String classname = matcher.group(1);
				int period = classname.lastIndexOf(".");
				if (period != -1)
					classname = classname.substring(period + 1);
				matcher.appendReplacement(linksReplaced, "\\\\op{" + classname + "}");
			}
			matcher.appendTail(linksReplaced);
			return linksReplaced.toString();
		} catch (Throwable e) {
			LogService.getGlobal().log(operatorName + " (" + clazz.getName() + "): " + e, LogService.ERROR);
			return "Cannot parse class comment: " + e;
		}
	}
}
