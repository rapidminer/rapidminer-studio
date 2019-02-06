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
package com.rapidminer.gui.tools.components;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;


/**
 * This button presents a possibility to select a color.
 * 
 * @author Sebastian Land
 */
public class ColorSelectionButton extends JButton {

	private static final long serialVersionUID = 1L;

	private Color color;

	private AbstractColorChooserPanel[] colorChoosers = null;

	private List<ColorSelectionListener> listeners = new LinkedList<ColorSelectionListener>();

	/**
	 * This method will create a new Color Selection Button which will open a dialog if pressed to
	 * select a new color. The default color can be passed.
	 * 
	 * The i18nkey refers to the dialog title, which is retrieved from gui.label.-key- while the
	 * button properties are refered from: gui.action.-key-.label = Which will be the caption
	 * gui.action.-key-.acc = The accelerator key used for menu entries gui.action.-key-.tip = Which
	 * will be the tool tip gui.action.-key-.mne = Which will give you access to the mnemonics key.
	 * Please make it the same case as in the label
	 */
	public ColorSelectionButton(final String i18nKey, Color defaultColor, Object... arguments) {
		this.color = defaultColor;

		setAction(new ResourceAction(i18nKey, arguments) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				Color newColor = showDialog(RapidMinerGUI.getMainFrame(), I18N.getGUILabel(i18nKey), color);
				if (newColor != null) {
					color = newColor;
					setIcon(new ColorIcon(color));
					notifyColorSelectionListeners(color);
				}
			}
		});
		setIcon(new ColorIcon(color));
	}

	public void addColorSelectionListener(ColorSelectionListener listener) {
		listeners.add(listener);
	}

	public void removeColorSelectionListener(ColorSelectionListener listener) {
		listeners.remove(listener);
	}

	private void notifyColorSelectionListeners(Color color) {
		for (ColorSelectionListener l : listeners) {
			l.colorSelected(color);
		}
	}

	/**
	 * This method can be used to retrieve the selected Color.
	 */
	public Color getSelectedColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
		setIcon(new ColorIcon(color));
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			setIcon(new ColorIcon(color));
		} else {
			ColorIcon colorIcon = new ColorIcon(new Color(235, 235, 235));
			colorIcon.setBorderColor(new Color(210, 210, 210));
			setIcon(colorIcon);
		}
	}

	public void setColorChooser(AbstractColorChooserPanel... colorChoosers) {
		this.colorChoosers = colorChoosers;
	}

	private Color showDialog(Component component, String title, Color initialColor) throws HeadlessException {
		final JColorChooser chooser = new JColorChooser(initialColor != null ? initialColor : Color.white);

		// configuring color chooser panel

		chooser.setPreviewPanel(new JPanel());
		if (colorChoosers != null) {
			chooser.setChooserPanels(colorChoosers);
		} else {
			chooser.removeChooserPanel(chooser.getChooserPanels()[0]);
			chooser.removeChooserPanel(chooser.getChooserPanels()[1]);
		}

		// creating dialog
		ColorTracker ok = new ColorTracker(chooser);
		JDialog dialog = JColorChooser.createDialog(component, title, true, chooser, ok, null);
		dialog.setVisible(true); // blocks until user brings dialog down...
		return ok.getColor();
	}

	/**
	 * Small helper class for the color selection dialog
	 */
	private static class ColorTracker implements ActionListener, Serializable {

		private static final long serialVersionUID = 486260520128499950L;
		JColorChooser chooser;
		Color color;

		public ColorTracker(JColorChooser c) {
			chooser = c;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			color = chooser.getColor();
		}

		public Color getColor() {
			return color;
		}
	}

}
