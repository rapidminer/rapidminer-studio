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
package com.rapidminer.gui.tools;

import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.tools.Tools;


/**
 * This class extends the editor pane in a way, so that more than one style sheet for HTML content
 * can be used. This is done for the price of only supporting HTML.
 *
 * @author Sebastian Land
 */
public class ExtendedHTMLJEditorPane extends JEditorPane {

	private static final long serialVersionUID = -1169198792942550655L;

	public ExtendedHTMLJEditorPane() {
		super();
	}

	public ExtendedHTMLJEditorPane(String url) throws IOException {
		super(url);
	}

	public ExtendedHTMLJEditorPane(String type, String text) {
		super(type, text);
	}

	@Override
	public EditorKit getEditorKitForContentType(String type) {
		return new ExtendedHTMLEditorKit();
	}

	public void installDefaultStylesheet() {
		installDefaultStylesheet(((HTMLEditorKit) getEditorKit()).getStyleSheet());
	}

	public static StyleSheet makeDefaultStylesheet() {
		StyleSheet css = new StyleSheet();
		installDefaultStylesheet(css);
		return css;
	}

	public static void installDefaultStylesheet(StyleSheet css) {
		css.addRule("body {font-family:Sans;font-size:12pt;}");
		css.addRule("h3 {margin:0; padding:0;margin-top:8px;margin-bottom:3px; }");
		// String hcolor =
		// Integer.toHexString(SwingTools.DARKEST_BLUE.darker().darker().darker().getRGB());
		// hcolor = hcolor.substring(2, 8);
		String hcolor = "446699";
		css.addRule("h4 {margin-bottom:1px; margin-top:2ex; padding:0; color:#" + hcolor + "; font-size:12px}");
		// css.addRule("h2, h3, h4 { border-width:3px; border-style:solid;
		// border-color:#"+Integer.toHexString(SwingTools.RAPID_I_ORANGE.getRGB())+"; }");
		css.addRule("p  {margin-top:0; margin-bottom:2ex; padding:0;}");
		// css.addRule("ul {margin-top:0; margin-bottom:1ex; list-style-image:url(" +
		// Tools.getResource("icons/help/circle.png") + "); }");
		css.addRule("ul.ports {margin-top:0; margin-bottom:1ex; list-style-image:url("
				+ Tools.getResource("icons/help/circle.png") + "); }");
		css.addRule("ul li {padding-bottom:1ex}");
		// css.addRule("li.outPorts {padding-bottom:0px}");
		css.addRule("ul.param_dep {margin-top:0; margin-bottom:1ex; list-style-type:none; list-style-image:none; }");
		// css.addRule("ul li ul {margin-top:0; margin-bottom:1ex; list-style-type:none;
		// list-style-image:none; }");
		// css.addRule("ul li small ul {margin-top:0; list-style-type:none; list-style-image:none;
		// }");
		css.addRule("li ul li {padding-bottom:0}");
		// css.addRule("a {text-decoration:none}");
		// css.addRule("a:hover {text-decoration:underline}");
		css.addRule("dt  {font-weight:bold;}");
		// css.addRule("a {text-decoration:underline; font-weight:bold;color:blue;}");
		css.addRule("hr  {color:red; background-color:red}");
		css.addRule("a {color:" + SwingTools.getColorHexValue(Colors.LINKBUTTON_REMOTE) + "}");
	}
}
