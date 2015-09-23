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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.gui.look.borders.TextFieldBorder;
import com.rapidminer.tools.I18N;


/**
 * This component combines a {@link JTextField} with a {@link ResourceAction}. The icon of the
 * action is shown on the right side of the textfield, and can be clicked to invoke the action. If
 * the text field is empty the action will not be painted and cannot be invoked.
 * 
 * @author Marco Boeck, Nils Woehler
 * 
 */
public class TextFieldWithAction extends JPanel {

	private static final long serialVersionUID = 1645009541373049543L;

	/** flag indicating if the user is hovering over the action icon */
	private boolean hovered;

	/***/
	private ImageIcon actionIcon = null;

	/***/
	private ImageIcon hoverActionIcon = null;

	/**
	 * Creates a new {@link TextFieldWithAction} instance.
	 * 
	 * @param field
	 *            the textfield into which the action icon should be placed
	 * @param action
	 *            the action to invoke when clicking the icon in the textfield
	 */
	public TextFieldWithAction(final JTextField field, final ResourceAction action) {

		this(field, action, null);
	}

	/**
	 * Creates a new {@link TextFieldWithAction} instance.
	 * 
	 * @param field
	 *            the textfield into which the action icon should be placed
	 * @param action
	 *            the action to invoke when clicking the icon in the textfield
	 * @param hoverAction
	 *            the action to invoke when hovering the icon in the textfield
	 */
	public TextFieldWithAction(final JTextField field, final ResourceAction action, final ImageIcon hoverIcon) {
		if (field == null) {
			throw new IllegalArgumentException("field must not be null!");
		}
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(field, gbc);

		actionIcon = SwingTools.createIcon("16/" + action.getIconName());
		hoverActionIcon = hoverIcon;

		final JLabel actionLabel = new JLabel(actionIcon) {

			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				// override this so the action is only visible, when there is text in the text field
				if (!(field.getText().isEmpty() || field.getText() == null)) {
					Graphics2D g2 = (Graphics2D) g;

					// only in the case there is no hover action icon given
					if (hoverActionIcon == null && hovered) {
						int xStart = 0;
						int yStart = 0;
						int xEnd = getWidth() - 1;
						int yEnd = getHeight() - 1;
						int arcWidth = 3;

						// fill background
						g2.setPaint(UIManager.getColor("Panel.background"));
						g2.fillRoundRect(xStart, yStart, xEnd, yEnd, arcWidth, arcWidth);

						// draw border
						g2.setPaint(SwingTools.RAPIDMINER_ORANGE);
						g2.drawRoundRect(xStart, yStart, xEnd, yEnd, arcWidth, arcWidth);

						g2.setPaint(Color.WHITE);
					}

					super.paintComponent(g2);
				}
			}
		};
		actionLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action." + action.getKey() + ".tip"));
		actionLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!(field.getText().isEmpty() || field.getText() == null) && SwingUtilities.isLeftMouseButton(e)) {
					action.actionPerformed(new ActionEvent(actionLabel, ActionEvent.ACTION_PERFORMED, "click"));
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				hovered = true;
				if (hoverActionIcon != null) {
					actionLabel.setIcon(hoverActionIcon);
				}
				actionLabel.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				hovered = false;
				if (hoverActionIcon != null) {
					actionLabel.setIcon(actionIcon);
				}
				actionLabel.repaint();
			}

		});

		field.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				actionLabel.repaint();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				actionLabel.repaint();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				actionLabel.repaint();
			}
		});

		gbc.insets = new Insets(0, 1, 0, 1);
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		add(actionLabel, gbc);

		// set panel background to textfield background
		setBackground(field.getBackground());
		// add textfield border to panel
		setBorder(new TextFieldBorder());
		// hide textfield border
		field.setBorder(null);
	}

}
