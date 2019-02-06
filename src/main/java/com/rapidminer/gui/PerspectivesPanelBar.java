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
package com.rapidminer.gui;

import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.plaf.LayerUI;


/**
 * The perspectives panel bar has a gray background and displays the perspectives as toggle buttons.
 * 
 * @author Nils Woehler
 * 
 */
public class PerspectivesPanelBar extends JPanel {

	private static final long serialVersionUID = 1L;

	private static class PerspectivesLayerUI extends LayerUI<JPanel> {

		private static final long serialVersionUID = 1L;

		private final JPanel contentPanel;

		public PerspectivesLayerUI(JPanel contentPanel) {
			this.contentPanel = contentPanel;
		}

		@Override
		public void paint(Graphics g, JComponent c) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackgroundColor());
			Rectangle rec = contentPanel.getBounds();
			g2.fillRoundRect((int) rec.getX(), (int) rec.getY(), rec.width + 15, rec.height, 25, 25);
			g2.setColor(SwingTools.RAPIDMINER_ORANGE);
			g2.drawRoundRect((int) rec.getX(), (int) rec.getY(), rec.width + 15, rec.height - 1, 25, 25);
			g2.dispose();
			super.paint(g, c);
		}

	}

	@SuppressWarnings("deprecation")
	private PerspectivesPanelBar(Perspectives perspectives) {
		setLayout(new GridBagLayout());
		setOpaque(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(1, 10, 2, 0);

		JToolBar workspaceToolBar = perspectives.getWorkspaceToolBar();
		workspaceToolBar.setBackground(getBackgroundColor());
		add(workspaceToolBar, gbc);
	}

	private static final Color getBackgroundColor() {
		Color lightGray = SwingTools.RAPIDMINER_LIGHT_GRAY;
		return new Color(lightGray.getRed(), lightGray.getGreen(), lightGray.getBlue(), 50);
	}

	/**
	 * Factory to create a perspectives panel bar which is covered by a JLayer.
	 *
	 * @param perspectives
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static final JLayer<JPanel> getPerspecitvesPanelBar(Perspectives perspectives) {
		PerspectivesPanelBar view = new PerspectivesPanelBar(perspectives);
		return new JLayer<>(view, new PerspectivesLayerUI(view));
	}
}
