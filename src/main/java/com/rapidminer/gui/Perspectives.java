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
package com.rapidminer.gui;

import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.WelcomeScreen;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.template.gui.TemplateView;
import com.vlsolutions.swing.docking.DockingConstants;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.RelativeDockablePosition;
import com.vlsolutions.swing.docking.ws.WSDesktop;
import com.vlsolutions.swing.docking.ws.WSDockKey;


/**
 * Collection of {@link Perspective}s that can be applied, saved, created.
 * 
 * @author Simon Fischer
 * 
 */
public class Perspectives extends ApplicationPerspectives {

	public static final String WELCOME = "welcome";
	public static final String RESULT = "result";
	public static final String DESIGN = "design";
	public static final String BUSINESS = "business";

	public Perspectives(DockingContext context) {
		super(context);
	}

	@Override
	protected void makePredefined() {
		addPerspective(WELCOME, false);
		restoreDefault(WELCOME);
		addPerspective(DESIGN, false);
		restoreDefault(DESIGN);
		addPerspective(RESULT, false);
		restoreDefault(RESULT);
		addPerspective(BUSINESS, false);
		restoreDefault(BUSINESS);
	}

	@Override
	protected void restoreDefault(String perspectiveName) {
		WSDockKey processPanelKey = new WSDockKey(ProcessPanel.PROCESS_PANEL_DOCK_KEY);
		WSDockKey propertyTableKey = new WSDockKey(OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY);
		WSDockKey resultsKey = new WSDockKey(ResultDisplay.RESULT_DOCK_KEY);
		WSDockKey repositoryKey = new WSDockKey(RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY);
		WSDockKey newOperatorEditorKey = new WSDockKey(NewOperatorEditor.NEW_OPERATOR_DOCK_KEY);
		WSDockKey operatorHelpKey = new WSDockKey(OperatorDocViewer.OPERATOR_HELP_DOCK_KEY);
		WSDockKey welcomeKey = new WSDockKey(WelcomeScreen.WELCOME_SCREEN_DOCK_KEY);
		WSDockKey templatesKey = new WSDockKey(TemplateView.TEMPLATES_DOCK_KEY);

		if (DESIGN.equals(perspectiveName)) {
			Perspective designPerspective = getPerspective(DESIGN);
			WSDesktop designDesktop = designPerspective.getWorkspace().getDesktop(0);
			designDesktop.clear();
			designDesktop.addDockable(processPanelKey);
			designDesktop.split(processPanelKey, propertyTableKey, DockingConstants.SPLIT_RIGHT, 0.8);
			designDesktop.split(propertyTableKey, operatorHelpKey, DockingConstants.SPLIT_BOTTOM, .66);
			designDesktop.split(processPanelKey, newOperatorEditorKey, DockingConstants.SPLIT_LEFT, 0.25);
			designDesktop.split(newOperatorEditorKey, repositoryKey, DockingConstants.SPLIT_BOTTOM, 0.5);
		} else if (RESULT.equals(perspectiveName)) {
			Perspective resultPerspective = getPerspective(RESULT);
			WSDesktop resultsDesktop = resultPerspective.getWorkspace().getDesktop(0);
			resultsDesktop.clear();
			resultsDesktop.addDockable(resultsKey);
			resultsDesktop.split(resultsKey, repositoryKey, DockingConstants.SPLIT_RIGHT, 0.8);
		} else if (WELCOME.equals(perspectiveName)) {
			Perspective welcomePerspective = getPerspective(WELCOME);
			WSDesktop welcomeDesktop = welcomePerspective.getWorkspace().getDesktop(0);
			welcomeDesktop.clear();
			welcomeDesktop.addDockable(welcomeKey);
		} else if (BUSINESS.equals(perspectiveName)) {
			Perspective designPerspective = getPerspective(BUSINESS);
			WSDesktop designDesktop = designPerspective.getWorkspace().getDesktop(0);
			designDesktop.clear();
			designDesktop.addDockable(templatesKey);
			designDesktop.addHiddenDockable(repositoryKey, RelativeDockablePosition.LEFT);
		} else {
			throw new IllegalArgumentException("Not a predevined perspective: " + perspectiveName);
		}
	}
}
