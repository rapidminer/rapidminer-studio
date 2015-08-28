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
package com.rapidminer.gui.tour.comic.gui;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.NotificationPopup;
import com.rapidminer.gui.tools.NotificationPopup.NotificationPopupListener;
import com.rapidminer.gui.tools.NotificationPopup.PopupLocation;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.ButtonBubble;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.tools.I18N;

import java.awt.Color;
import java.awt.Container;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * Gui helper methods for the comics.
 * 
 * @author Marco Boeck
 * 
 */
public class ComicGuiTools {

	/** the success icon */
	private static final ImageIcon SUCCESS_ICON = SwingTools.createIcon("64/"
			+ I18N.getMessage(I18N.getGUIBundle(), "gui.notifaction.comic.finish.icon"));

	/** the delay in ms before the success notification fades away */
	private static final int DELAY_SUCCESS_BEFORE_FADE = 30000;

	/**
	 * Creates and shows the comic finishing notification.
	 * 
	 * @param episode
	 *            the episode for which to display the finish dialog.
	 * @return
	 */
	public static NotificationPopup createAndShowComicFinishedPopup(final AbstractEpisode episode) {
		// setup notifcation
		final JPanel notificationPanel = new JPanel() {

			private static final long serialVersionUID = -3252681210360686197L;

			@Override
			public void paintComponent(final Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				Color bg = getBackground();
				GradientPaint gp = new GradientPaint(0, 0, bg.brighter(), 0, getHeight(), bg);
				g2.setPaint(gp);
				g2.fillRect(0, 0, getWidth(), getHeight());
				super.paintComponent(g2);
			}
		};
		notificationPanel.setLayout(new GridBagLayout());
		notificationPanel.setOpaque(false);
		GridBagConstraints gbc = new GridBagConstraints();

		StringBuilder goalList = new StringBuilder();
		goalList.append("<ul style=\"font-style: italic;\">");
		for (String goal : episode.getGoals().split(";")) {
			goalList.append("<li>");
			goalList.append(goal.trim());
			goalList.append("</li>");
		}
		goalList.append("</ul>");

		JLabel notifactionLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.notifaction.comic.finish.label",
				episode.getTitle(), goalList.toString()));
		notifactionLabel.setIcon(SUCCESS_ICON);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 10, 0, 10);
		gbc.gridwidth = 2;
		notificationPanel.add(notifactionLabel, gbc);

		LinkButton nextButton = new LinkButton(new ResourceAction("comic.start_another_comic", true) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				ComicDialog.getInstance().showDialog();

				// small hack to close the popup once the link has been clicked because we don't
				// have a reference for it here
				Container notificationPopup = SwingUtilities.getAncestorOfClass(NotificationPopup.class, notificationPanel);
				if (notificationPopup != null) {
					((NotificationPopup) notificationPopup).dispose();
				}
			}
		});
		gbc.insets = new Insets(0, SUCCESS_ICON.getIconWidth() + 14, 10, 10);
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 1;
		notificationPanel.add(nextButton, gbc);

		LinkButton tryButton = new LinkButton(new ResourceAction("comic.let_me_try", true) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				// small hack to close the popup once the link has been clicked because we don't
				// have a reference for it here
				Container notificationPopup = SwingUtilities.getAncestorOfClass(NotificationPopup.class, notificationPanel);
				if (notificationPopup != null) {
					((NotificationPopup) notificationPopup).dispose();
				}

				// let user use his built process
				Process builtProcess = episode.getBuiltProcessOrNull();
				if (builtProcess != null) {
					RapidMinerGUI.getMainFrame().setProcess(builtProcess, false);
				}
			}
		});
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(0, 0, 10, 20);
		notificationPanel.add(tryButton, gbc);

		NotificationPopupListener listener = new NotificationPopupListener() {

			@Override
			public void popupClosed(NotificationPopup popup) {
				showComicHintBubble();
			}
		};

		return NotificationPopup.showFadingPopup(notificationPanel, RapidMinerGUI.getMainFrame().getContentPane(),
				PopupLocation.CENTER, DELAY_SUCCESS_BEFORE_FADE, 0, 0, BorderFactory.createLineBorder(Color.GRAY, 1, false),
				listener);
	}

	/**
	 * Shows a bubble window at the location of the comic tutorial button. <br/>
	 * If the comic tutorial dialog is currently open, does nothing.
	 */
	public static void showComicHintBubble() {
		// show hint bubble if user did not click on "next tutorial" link button
		if (!ComicDialog.getInstance().isVisible()) {
			BubbleWindow moreComicsBubble = new ButtonBubble(RapidMinerGUI.getMainFrame(), null, AlignedSide.BOTTOM,
					"comic_button_comic", "comic_dropdown");
			moreComicsBubble.setVisible(true);
		}
	}
}
