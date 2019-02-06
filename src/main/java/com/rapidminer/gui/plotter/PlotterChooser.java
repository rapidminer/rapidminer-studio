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
package com.rapidminer.gui.plotter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.rapidminer.gui.popup.PopupAction;
import com.rapidminer.gui.tools.ListHoverHelper;
import com.rapidminer.gui.tools.MenuShortcutJList;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.LogService;


/**
 * Selection field for the available {@link Plotter} which shows preview images for all plotters.
 * The look and feel is similar to a Combobox. A selection informs all listeners at
 * {@link PlotterControlPanel}.
 *
 * @author David Arnu, Michael Knopf
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotterChooser extends JButton {

	private static final long serialVersionUID = 1L;

	/**
	 * The gap of the drop down arrow button
	 */
	private static final int GAP_RIGHT = 11;

	private static final int SMALL_ICON_SIZE = 50;

	private static final int ICON_SIZE = 100;

	private static final double MIN_RESOLUTION_HEIGHT = 800;

	private boolean smallIcons = false;

	private final class PlotterListCellRenderer<E> extends JPanel implements ListCellRenderer<E> {

		private static final long serialVersionUID = 1L;

		// cell components
		private JLabel label;

		// styling
		private final Color defaultBackground; // see constructor
		private final Color hoverBackground = Color.WHITE;
		private final int borderWidth = 3;
		private final Border defaultrBorder = BorderFactory.createEmptyBorder(this.borderWidth, this.borderWidth,
				this.borderWidth, this.borderWidth);
		private final Border hoverBorder = BorderFactory.createLineBorder(this.hoverBorderColor, this.borderWidth);
		private final Color hoverBorderColor = Color.DARK_GRAY;
		private final Font defaultFont; // see constructor
		private final Font hoverFont; // see constructor

		/**
		 * Defines the layout of the panel once for all cells. Instances are reused for cell
		 * rendering.
		 */
		public PlotterListCellRenderer() {
			super();
			// setup label
			this.label = new JLabel();
			this.label.setHorizontalAlignment(SwingConstants.CENTER);
			this.label.setVerticalAlignment(SwingConstants.TOP);
			this.label.setHorizontalTextPosition(SwingConstants.CENTER);
			this.label.setVerticalTextPosition(SwingConstants.BOTTOM);

			this.add(this.label);
			// setup colors / fonts
			this.defaultBackground = this.getBackground();
			this.defaultFont = this.label.getFont();
			this.hoverFont = this.defaultFont.deriveFont(Font.BOLD);
		}

		/**
		 * Updates panel and label for the given cell.
		 */
		@Override
		public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected,
				boolean cellHasFocus) {
			String plotterName = (String) value;
			// display selected variant of the icon if the cell is selected
			// or in a hover state
			Icon icon;
			if (ListHoverHelper.index(list) == index) {
				icon = getIcon(plotterName, true);
			} else {
				icon = getIcon(plotterName, isSelected);
			}
			// update panel and label depending on cell state
			label.setIcon(icon);
			label.setText(plotterName);
			if (isSelected) {
				this.setBorder(this.hoverBorder);
				this.setBackground(this.hoverBackground);
				this.label.setFont(this.hoverFont);
			} else {
				this.setBorder(this.defaultrBorder);
				// since the same panel and label is used for all cells, we
				// have to 'reset' the styles to their default values
				this.setBackground(this.defaultBackground);
				this.label.setFont(this.defaultFont);
			}
			this.requestFocusInWindow();
			return this;
		}

		private Icon getIcon(String plotterName, boolean selected) {
			// check to decide which icon size should be loaded
			StringBuilder builder = new StringBuilder("icons/chartPreview/");
			builder.append(isSmallIconsUsed() ? SMALL_ICON_SIZE : ICON_SIZE).append('/');
			builder.append(plotterName.replace(' ', '_'));
			if (!selected) {
				builder.append("-grey");
			}
			builder.append(".png");
			return SwingTools.createImage(builder.toString());
		}
	}

	private JList<String> plotterList = new MenuShortcutJList<>(new DefaultListModel<>(), false);

	public PlotterChooser() {
		super();

		ListHoverHelper.install(plotterList);

		smallIcons = isResolutionTooSmall();

		plotterList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		plotterList.setVisibleRowCount(5);
		plotterList.setCellRenderer(new PlotterListCellRenderer<>());
		plotterList.setBackground(UIManager.getColor("Panel.background"));
		plotterList.setSelectionForeground(Color.BLACK);
		final PopupAction popupAction = new PopupAction("choose_plotter", plotterList);
		setAction(popupAction);

		this.addActionListener(e -> {
			if (smallIcons != isResolutionTooSmall()) {
				smallIcons = !smallIcons; // toggle the type of icons
				// if the resolution has changed, create a new panel to support this resolution
				plotterList.setCellRenderer(new PlotterListCellRenderer<>());
			}
		});

		plotterList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent arg0) {

				popupAction.focusLost();
				fireSelectionEvent();
			}
		});
		plotterList.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				int key = arg0.getKeyCode();
				if (key == KeyEvent.VK_ENTER) {
					popupAction.focusLost();
					fireSelectionEvent();
				} else if (key == KeyEvent.VK_ESCAPE) {
					// close popup without doing anything
					popupAction.focusLost();
				}
			}
		});
	}

	public void setSettings(PlotterConfigurationModel plotterSettings) {
		populateList(plotterSettings);
	}

	private void populateList(PlotterConfigurationModel plotterSettings) {
		((DefaultListModel<?>) plotterList.getModel()).clear();
		plotterSettings.getAvailablePlotters().forEach((plotterName, plotterClass) -> {
			try {
				if (plotterClass != null) {
					((DefaultListModel<String>) plotterList.getModel()).addElement(plotterName);
				}
			} catch (IllegalArgumentException | SecurityException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.plotter.PlotterControlPanel.instatiating_plotter_error", plotterName);
			}
		});

	}

	public void removeAllItems() {
		((DefaultListModel<String>) plotterList.getModel()).clear();

	}

	public void addItem(String item) {
		((DefaultListModel<String>) plotterList.getModel()).addElement(item);
		if (plotterList.getModel().getSize() == 1) {
			plotterList.setSelectedIndex(0);
			updateButtonText();
		}
	}

	public void setSelectedItem(String plotterName) {
		plotterList.setSelectedValue(plotterName, true);
		updateButtonText();

	}

	private void fireSelectionEvent() {
		updateButtonText();
		fireItemStateChanged(new ItemEvent(this, plotterList.getSelectedIndex(), plotterList.getSelectedIndex(),
				ItemEvent.SELECTED));
	}

	private void updateButtonText() {
		setText((String) getSelectedItem());
	}

	public Object getSelectedItem() {
		return plotterList.getSelectedValue();
	}

	/**
	 * Query if the small plot preview icons are used
	 *
	 * @return true, if the small icons are used
	 */
	public boolean isSmallIconsUsed() {
		return smallIcons;
	}

	/**
	 * Set to true if the small plot preview icons should be used
	 *
	 * @param smallIcons
	 *            true, for the small icons to be used
	 */
	public void setUseSmallIcons(boolean smallIcons) {
		this.smallIcons = smallIcons;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		GeneralPath arrow = new GeneralPath();
		int h = 2;
		int w = 4;
		arrow.moveTo(getWidth() - GAP_RIGHT - w, getHeight() / 2);
		arrow.lineTo(getWidth() - GAP_RIGHT + w, getHeight() / 2);
		arrow.lineTo(getWidth() - GAP_RIGHT, getHeight() / 2 + 2 * h);
		arrow.closePath();

		Graphics2D g2 = (Graphics2D) g.create();
		if (isEnabled()) {
			g2.setColor(Color.BLACK);
		} else {
			g2.setColor(Color.GRAY);
		}
		g2.fill(arrow);
		g2.dispose();
	}

	/**
	 *
	 * @return Returns true if a display has a too small resolution
	 */
	private boolean isResolutionTooSmall() {

		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

		GraphicsDevice[] allScreens = env.getScreenDevices();
		// check for all screen devices their display height
		for (GraphicsDevice screen : allScreens) {
			if (screen.getDefaultConfiguration().getBounds().getHeight() < MIN_RESOLUTION_HEIGHT) {
				return true;
			}
		}
		return false; // big icons are supported
	}

}
