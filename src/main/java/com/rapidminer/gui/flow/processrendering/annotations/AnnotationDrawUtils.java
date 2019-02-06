/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.gui.flow.processrendering.annotations;

import java.awt.Font;
import java.io.IOException;
import java.io.StringWriter;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import org.apache.commons.lang.StringEscapeUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * Utility methods for {@link WorkflowAnnotation}s.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationDrawUtils {

	/** surrounding HTML for annotations where styling information can be set */
	private static final String ANNOTATION_HTML_FORMAT = "<div id=\"anno_style_div\" style=\"padding: %dpx; %s\">%s</div>";

	/** this is removed from the beginning of the document */
	private static final String ANNOTATION_HTML_FORMATTING_START_REGEX = "\\s*<html>\\s*(<head>)?\\s*.*\\s*(</head>)?\\s*<body>\\s*";

	/** this is removed from the end of the document */
	private static final String ANNOTATION_HTML_FORMATTING_END_REGEX = "\\s*</body>\\s*</html>\\s*";

	/** divs are forbidden because we use them to style the annotation globally */
	private static final String ANNOTATION_HTML_FORMATTING_MISC_DIV_START = "<div(.*?)>";

	/** divs are forbidden because we use them to style the annotation globally */
	private static final String ANNOTATION_HTML_FORMATTING_MISC_DIV_END = "</div>";

	/** the signal we fake everytime the user enters a newline */
	public static final String ANNOTATION_HTML_NEWLINE_SIGNAL = "\r";

	/** the font for the annotations */
	public static final Font ANNOTATION_FONT = FontTools.getFont("Open Sans", Font.PLAIN, 13);

	/**
	 * Private constructor which throws if called.
	 */
	private AnnotationDrawUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Creates a HTML document which applies the style information from the given annotation.
	 *
	 * @param annotation
	 *            the annotation for which to create the full HTML document
	 * @return the HTML document as a string
	 */
	public static String createStyledCommentString(final WorkflowAnnotation annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("annotation must not be null!");
		}
		return createStyledCommentString(annotation.getComment(), annotation.getStyle());
	}

	/**
	 * Creates a HTML document which applies the style information from the given annotation.
	 *
	 * @param comment
	 *            the comment for which to create the full HTML document
	 * @param style
	 *            the style which should be used to style the comment
	 * @return the HTML document as a string
	 */
	public static String createStyledCommentString(final String comment, final AnnotationStyle style) {
		if (comment == null) {
			throw new IllegalArgumentException("comment must not be null!");
		}
		if (style == null) {
			throw new IllegalArgumentException("style must not be null!");
		}
		return String.format(ANNOTATION_HTML_FORMAT, style.getPadding(), style.getAnnotationAlignment().getCSS(), comment);
	}

	/**
	 * This method removes styling information from the HTML comment.
	 * <p>
	 * <strong>Attention:</strong> It is not possible to use Regex to parse arbitrary HTML!! This
	 * will fail horribly in case the user entered complex HTML! Works fine for simple HTML without
	 * tables etc.
	 * </p>
	 *
	 * @param comment
	 *            the comment to remove styling information from
	 * @return the sanitized comment
	 */
	public static String removeStyleFromComment(final String comment) {
		if (comment == null) {
			throw new IllegalArgumentException("comment must not be null!");
		}
		String newComment = comment.replaceAll(ANNOTATION_HTML_FORMATTING_START_REGEX, "");
		newComment = newComment.replaceAll(ANNOTATION_HTML_FORMATTING_END_REGEX, "");
		// replace potential occurances of any divs
		newComment = newComment.replaceAll(ANNOTATION_HTML_FORMATTING_MISC_DIV_START, "");
		newComment = newComment.replaceAll(ANNOTATION_HTML_FORMATTING_MISC_DIV_END, "");

		// IMPORTANT: convert newline signal to HTML linebreak
		newComment = newComment.replaceAll(ANNOTATION_HTML_NEWLINE_SIGNAL, "<br/>");

		// remove superflous whitespaces
		newComment = newComment.replaceAll("\n", "");
		newComment = newComment.replaceAll("\\s+", " ");
		newComment = newComment.trim();
		return newComment;
	}

	/**
	 * Returns plain text from the editor.
	 *
	 * @param editor
	 *            the editor from which to take the text.
	 * @param onlySelected
	 *            if {@code true} will only return the selected text
	 * @return the text of the editor converted to plain text
	 * @throws BadLocationException
	 * @throws IOException
	 */
	public static String getPlaintextFromEditor(final JEditorPane editor, final boolean onlySelected)
			throws IOException, BadLocationException {
		if (editor == null) {
			throw new IllegalArgumentException("editor must not be null!");
		}
		HTMLDocument document = (HTMLDocument) editor.getDocument();
		StringWriter writer = new StringWriter();
		int start = 0;
		int length = document.getLength();
		if (onlySelected) {
			start = editor.getSelectionStart();
			length = editor.getSelectionEnd() - start;
		}
		editor.getEditorKit().write(writer, document, start, length);
		String text = writer.toString();
		text = AnnotationDrawUtils.removeStyleFromComment(text);
		// switch <br> and <br/> to actual newline (current system)
		text = text.replaceAll("<br.*?>", System.lineSeparator());
		// kill all other html tags
		text = text.replaceAll("\\<.*?>", "");
		text = StringEscapeUtils.unescapeHtml(text);
		return text;
	}

	/**
	 * Calculates the preferred height of an editor pane with the given fixed width for the
	 * specified string.
	 *
	 * @param comment
	 *            the annotation comment string
	 * @param width
	 *            the width of the content
	 * @return the preferred height given the comment
	 */
	public static int getContentHeight(final String comment, final int width, final Font font) {
		if (comment == null) {
			throw new IllegalArgumentException("comment must not be null!");
		}
		// do not create Swing components for headless mode
		if (RapidMiner.getExecutionMode().isHeadless()) {
			return 0;
		}
		JEditorPane dummyEditorPane = new JEditorPane("text/html", "");
		dummyEditorPane.setText(comment);
		dummyEditorPane.setBorder(null);
		dummyEditorPane.setSize(width, Short.MAX_VALUE);
		dummyEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		dummyEditorPane.setFont(font);

		// height is not exact. Multiply by magic number to get a more fitting value...
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX
				|| SystemInfoUtilities.getOperatingSystem() == OperatingSystem.UNIX
				|| SystemInfoUtilities.getOperatingSystem() == OperatingSystem.SOLARIS) {
			return (int) (dummyEditorPane.getPreferredSize().getHeight() * 1.05f);
		} else {
			return (int) dummyEditorPane.getPreferredSize().getHeight();
		}
	}
}
