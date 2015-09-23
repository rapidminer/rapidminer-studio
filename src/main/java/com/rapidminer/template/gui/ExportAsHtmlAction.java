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
package com.rapidminer.template.gui;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.template.TemplateController;
import com.rapidminer.tools.LogService;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;


/**
 * Exports the report as an HTML file.
 * 
 * @author Simon Fischer
 * 
 */
public class ExportAsHtmlAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final TemplateController controller;

	public ExportAsHtmlAction(TemplateController controller) {
		super(true, "template.export_html");
		this.controller = controller;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final File file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), "template.export_html", null, false, "html",
				"HTML files");
		if (file != null) {
			new ProgressThread("template.export_html") {

				@Override
				public void run() {
					try {
						controller.exportHTML(file);
					} catch (Exception e1) {
						LogService.getRoot().log(Level.WARNING, "Failed to export HTML: " + e1, e1);
						// Safe to call from any thread
						SwingTools.showSimpleErrorMessage("template_export_html_failed", e1);
					}
					try {
						Desktop.getDesktop().browse(file.toURI());
					} catch (IOException e) {
						LogService.getRoot().log(Level.WARNING, "Failed to open browser: " + e, e);
					}
				}
			}.start();
		}
	}
}
