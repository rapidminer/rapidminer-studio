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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.bubble.BubbleWindow;
import com.rapidminer.gui.tools.bubble.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tools.bubble.BubbleWindow.BubbleListener;
import com.rapidminer.gui.tools.bubble.BubbleWindow.BubbleStyle;
import com.rapidminer.gui.tools.bubble.ComponentBubbleWindow;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Provides a beta mode label and a separator that are only visible when beta features are
 * activated. The label is associated with a warning bubble that is shown when beta features are
 * activated or the label is clicked.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
class BetaFeaturesIndicator {

	/** The orange color used in the pylon icon */
	private static final Color ICON_COLOR = new Color(207, 111, 11);

	private final JLabel modeLabel;
	private final JSeparator separator;

	private boolean bubbleOpen;

	private final BubbleListener closeListener = new BubbleListener() {

		@Override
		public void bubbleClosed(BubbleWindow bw) {
			bubbleOpen = false;
		}

		@Override
		public void actionPerformed(BubbleWindow bw) {
			// not needed
		}

	};

	private final ParameterChangeListener betaFeaturesListener = new ParameterChangeListener() {

		@Override
		public void informParameterChanged(String key, String value) {
			if (RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES.equals(key)) {
				if (Boolean.parseBoolean(value)) {
					modeLabel.setVisible(true);
					separator.setVisible(true);
					logActivation(true);
				} else {
					modeLabel.setVisible(false);
					separator.setVisible(false);
					logActivation(false);
				}
				// update the process in order to update parameters depending on the beta mode
				RapidMinerGUI.getMainFrame().fireProcessUpdated();
			}
		}

		@Override
		public void informParameterSaved() {
			// do nothing
		}

	};

	/**
	 * Creates a indicator for activation of beta features.
	 */
	BetaFeaturesIndicator() {
		separator = new JSeparator(JSeparator.VERTICAL);
		modeLabel = new ResourceLabel("setting.activated_beta_features");
		modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
		modeLabel.setForeground(ICON_COLOR);
		modeLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				showBetaBubble();
			}
		});

		ParameterService.registerParameterChangeListener(betaFeaturesListener);
		if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_BETA_FEATURES))) {
			modeLabel.setVisible(true);
			separator.setVisible(true);
		} else {
			modeLabel.setVisible(false);
			separator.setVisible(false);
		}
	}

	/**
	 * @return a beta mode label that is only visible when beta features are activated
	 */
	JLabel getModeLabel() {
		return modeLabel;
	}

	/**
	 * @return a separator that is only visible when beta features are activated
	 */
	JSeparator getModeSeparator() {
		return separator;
	}

	/**
	 * Shows bubble that warns about beta features.
	 */
	private void showBetaBubble() {
		if (bubbleOpen) {
			return;
		}
		bubbleOpen = true;
		SwingUtilities.invokeLater(() -> {
			BubbleWindow bubble = new ComponentBubbleWindow(modeLabel, BubbleStyle.WARNING,
					ApplicationFrame.getApplicationFrame(), AlignedSide.TOP, "setting.activated_beta_features", null, null,
					false, true, null);
			bubble.addBubbleListener(closeListener);
			bubble.setVisible(true);
		});
	}

	/**
	 * Logs the (de)activation of the beta features.
	 */
	private void logActivation(boolean activated) {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_BETA_FEATURES,
				ActionStatisticsCollector.VALUE_BETA_FEATURES_ACTIVATION, String.valueOf(activated));
	}

}
