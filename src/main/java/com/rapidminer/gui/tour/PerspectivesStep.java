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
package com.rapidminer.gui.tour;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;

import java.awt.Window;


/**
 * This subclass of Step lets the user open a specific perspective.
 * 
 * @author Thilo Kamradt
 * 
 */
public class PerspectivesStep extends Step {

	public static final int PERSPECTIVE_WELCOME = 0;
	public static final int PERSPECTIVE_DESIGN = 1;
	public static final int PERSPECTIVE_RESULT = 2;

	private String i18nKey, buttonKey;
	private final Window owner = RapidMinerGUI.getMainFrame();
	private final AlignedSide alignment = AlignedSide.BOTTOM;
	private boolean showMe;
	private final int perspectiveIndex;
	private String name;
	private final PerspectiveChangeListener perspectiveListener;

	/**
	 * @param perspective
	 *            the perspective which should be shown at the end of the Step. (0 for
	 *            WelcomePerspective, 1 for DesignPerspective, 2 for ResultPerspective)
	 */
	public PerspectivesStep(final int perspective) {
		this(perspective, null);
	}

	public PerspectivesStep(final int perspective, final String i18nKey) {
		if (perspective < 0 || perspective > 2) {
			throw new IllegalArgumentException("the parameter perspective must be bigger than -1 and smaller than 3");
		}
		this.perspectiveIndex = perspective;
		switch (perspective) {
			case PERSPECTIVE_WELCOME:
				this.i18nKey = i18nKey == null ? "changeToWelcome" : i18nKey;
				buttonKey = "workspace_welcome";
				name = "welcome";
				break;
			case PERSPECTIVE_RESULT:
				this.i18nKey = i18nKey == null ? "changeToResult" : i18nKey;
				buttonKey = "workspace_result";
				name = "results";
				break;
			case PERSPECTIVE_DESIGN:
			default:
				this.i18nKey = i18nKey == null ? "changeToDesign" : i18nKey;
				buttonKey = "workspace_design";
				name = "design";
		}
		this.perspectiveListener = new PerspectiveChangeListener() {

			@Override
			public void perspectiveChangedTo(final Perspective perspective) {
				if (bubble != null) {
					if (perspectiveIndex == PERSPECTIVE_WELCOME && "welcome".equals(perspective.getName())) {
						bubble.fireEventActionPerformed();
					} else if (perspectiveIndex == PERSPECTIVE_DESIGN && "design".equals(perspective.getName())) {
						bubble.fireEventActionPerformed();
					} else if (perspectiveIndex == PERSPECTIVE_RESULT && "result".equals(perspective.getName())) {
						bubble.fireEventActionPerformed();
					}
				}
			}
		};
	}

	/**
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		switch (perspectiveIndex) {
			case 0:
				showMe = !(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName()
						.equals("welcome"));
				break;
			case 2:
				showMe = !(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("result"));
				break;
			case 1:
			default:
				showMe = !(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("design"));
		}
		if (showMe) {
			bubble = new ButtonBubble(owner, null, alignment, i18nKey, buttonKey, name);
			RapidMinerGUI.getMainFrame().getPerspectives().addPerspectiveChangeListener(perspectiveListener);
		}
		return showMe;
	}

	@Override
	protected void stepCanceled() {
		// we don't need to do anything because the only have a ButtonListener which will be removed
		// by the BubbleWindow
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

	@Override
	protected void conditionComplied() {
		RapidMinerGUI.getMainFrame().getPerspectives().removePerspectiveChangeListener(perspectiveListener);
		super.conditionComplied();
	}

}
