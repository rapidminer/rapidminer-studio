/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.actions;

import com.rapidminer.gui.OperatorDocViewer;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.plugin.Plugin;

import java.awt.event.ActionEvent;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;


/**
 * 
 * @author Miguel Buescher
 * 
 */
public class ShowHelpTextInBrowserAction extends ResourceAction {

	private static final String DOUBLE_POINT = ":";
	private final OperatorDocViewer operatorDocViewer;
	// private static final String[] browsers = new String[] { "iexplorer" };
	public static final String WIKI_URL_FOR_OPERATORS = "http://rapid-i.com/wiki/index.php?title=";

	public ShowHelpTextInBrowserAction(boolean smallIcon, String i18nKey, Object[] i18nArgs,
			OperatorDocViewer operatorDocViewer) {
		super(smallIcon, i18nKey, i18nArgs);
		this.operatorDocViewer = operatorDocViewer;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 909390054503086861L;

	@Override
	public void actionPerformed(ActionEvent e) {

		OperatorDescription operatorDescription = this.operatorDocViewer.getDisplayedOperator().getOperatorDescription();
		Plugin provider = operatorDescription.getProvider();
		String prefix = StringUtils.EMPTY;
		if (provider != null) {
			prefix = provider.getPrefix();
			prefix = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1) + DOUBLE_POINT;
		}
		String url = WIKI_URL_FOR_OPERATORS + prefix
				+ this.operatorDocViewer.getDisplayedOperatorDescName().replaceAll(" ", "_");

		try {
			RMUrlHandler.browse(java.net.URI.create(url));
		} catch (IOException e2) {
			SwingTools.showSimpleErrorMessage("cannot_open_browser", e2);
		}
	}
}
