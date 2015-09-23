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
package com.rapidminer.gui.tools.components;

import com.rapidminer.gui.tools.ViewToolBar;
import com.vlsolutions.swing.toolbars.VLToolBar;

import java.awt.CardLayout;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;


/**
 * 
 * The arrow button of this DropdownButton can be hidden and shown without the whole component
 * resizing.
 * 
 * @author Philipp Kersting
 * 
 */
public abstract class HidableDropDownButton extends DropDownButton {

	private static final long serialVersionUID = 1L;

	private final JToolBar arrowButtonPanel = new JToolBar();
	private final JPanel emptyPanel = new JPanel();

	public HidableDropDownButton(Action mainAction, Action arrowAction, boolean showText) {
		super(mainAction, arrowAction, showText);
		arrowButtonPanel.setLayout(new CardLayout());
		arrowButtonPanel.setFloatable(false);
		arrowButtonPanel.setBorder(null);
		arrowButton.setBorderPainted(true);
		emptyPanel.setOpaque(false);
		arrowButtonPanel.setOpaque(false);
	}

	public HidableDropDownButton(Action action, boolean showText) {
		this(action, null, showText);
	}

	public HidableDropDownButton(Action action) {
		this(action, null, false);
	}

	@Override
	public JButton addToToolBar(JToolBar toolbar) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton);
		toolbar.add(arrowButtonPanel);
		return mainButton;
	}

	@Override
	public JButton addToToolBar(JToolBar toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButtonPanel, arrowButtonConstraints);
		return mainButton;
	}

	@Override
	public JButton addToToolbar(JPanel toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButtonPanel, arrowButtonConstraints);
		return mainButton;
	}

	@Override
	public JButton addToToolBar(VLToolBar toolbar) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton);
		toolbar.add(arrowButtonPanel);
		return mainButton;
	}

	@Override
	public JButton addToToolBar(ViewToolBar toolbar, int alignment) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, alignment);
		toolbar.add(arrowButtonPanel, alignment);
		return mainButton;
	}

	@Override
	public JButton addToToolbar(JToolBar toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButtonPanel, arrowButtonConstraints);
		return mainButton;

	}

	@Override
	public JButton addToFlowLayoutPanel(JPanel panel) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		panel.add(mainButton);
		panel.add(arrowButtonPanel);
		return mainButton;
	}

	public void setArrowButtonVisible(boolean b) {
		CardLayout cl = (CardLayout) arrowButtonPanel.getLayout();
		if (b) {
			cl.first(arrowButtonPanel);
		} else {
			cl.last(arrowButtonPanel);
		}
	}

	@Override
	public void addArrowButtonMouseListener(MouseListener l) {
		super.addArrowButtonMouseListener(l);
		emptyPanel.addMouseListener(l);
	}

	@Override
	public void removeArrowButtonMouseListener(MouseListener l) {
		super.removeMouseListener(l);
		emptyPanel.removeMouseListener(l);
	}

}
