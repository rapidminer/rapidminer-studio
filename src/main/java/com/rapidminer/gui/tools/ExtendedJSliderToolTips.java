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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This class handles the tool tips for JSliders.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ExtendedJSliderToolTips {

	public static void enableSliderToolTips(final JSlider slider) {
		slider.addChangeListener(new ChangeListener() {

			private boolean adjusting = false;
			private String oldTooltip;

			@Override
			public void stateChanged(ChangeEvent e) {
				if (slider.getModel().getValueIsAdjusting()) {
					if (!adjusting) {
						oldTooltip = slider.getToolTipText();
						adjusting = true;
					}
					slider.setToolTipText(String.valueOf(slider.getValue()));
					hideToolTip(slider);
					postToolTip(slider);
				} else {
					hideToolTip(slider);
					slider.setToolTipText(oldTooltip);
					adjusting = false;
					oldTooltip = null;
				}
			}
		});
	}

	public static void postToolTip(JComponent comp) {
		Action action = comp.getActionMap().get("postTip");
		if (action == null) {
			return;
		}
		ActionEvent ae = new ActionEvent(comp, ActionEvent.ACTION_PERFORMED, "postTip", EventQueue.getMostRecentEventTime(),
				0);
		action.actionPerformed(ae);
	}

	public static void hideToolTip(JComponent comp) {
		Action action = comp.getActionMap().get("hideTip");
		if (action == null) {
			return;
		}
		ActionEvent ae = new ActionEvent(comp, ActionEvent.ACTION_PERFORMED, "hideTip", EventQueue.getMostRecentEventTime(),
				0);
		action.actionPerformed(ae);
	}
}
