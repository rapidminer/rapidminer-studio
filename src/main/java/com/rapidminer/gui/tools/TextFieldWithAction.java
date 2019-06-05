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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.borders.TextFieldBorder;
import com.rapidminer.gui.tools.ResourceAction.IconType;
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

	private ImageIcon actionIcon = null;

	private ImageIcon hoverActionIcon = null;

	private ImageIcon defaultIcon = null;
	private ImageIcon forceIcon = null;

	private JTextField field;
	private JLabel actionLabel;

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
	 * @param hoverIcon
	 *            the icon to display when hovering over the textfield
	 */
	public TextFieldWithAction(final JTextField field, final ResourceAction action, final ImageIcon hoverIcon) {
		if (field == null) {
			throw new IllegalArgumentException("field must not be null!");
		}
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}
		this.field = field;

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(field, gbc);

		actionIcon = SwingTools.createIcon("16/" + action.getIconName(), action.getIconType() == IconType.MONO);
		hoverActionIcon = hoverIcon;

		actionLabel = new JLabel(actionIcon) {

			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;

				// force icon set? Always show that.
				if (forceIcon != null) {
					super.paintComponent(g2);
					return;
				}

				// override this so the action is only visible, when there is text in the text field
				if (!(field.getText() == null || field.getText().isEmpty())) {

					// only in the case there is no hover action icon given
					if (hoverActionIcon == null && hovered) {
						int xStart = 0;
						int yStart = 0;
						int xEnd = getWidth();
						int yEnd = getHeight();

						// fill background
						g2.setPaint(Colors.TEXTFIELD_BACKGROUND);
						g2.fillRect(xStart, yStart, xEnd, yEnd);

					}

					super.paintComponent(g2);
				} else if (defaultIcon != null) {
					// no text but a default icon? Paint it.
					defaultIcon.paintIcon(this, g2, 0, 0);
				}
			}
		};
		actionLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action." + action.getKey() + ".tip"));
		actionLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!(field.getText().isEmpty() || field.getText() == null) && SwingUtilities.isLeftMouseButton(e)) {
					action.actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, "click"));
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (forceIcon != null) {
					return;
				}

				hovered = true;
				if (hoverActionIcon != null) {
					actionLabel.setIcon(hoverActionIcon);
				}
				actionLabel.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (forceIcon != null) {
					return;
				}

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

		field.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				TextFieldWithAction.this.repaint();
			}

			@Override
			public void focusGained(FocusEvent e) {
				TextFieldWithAction.this.repaint();
			}
		});

		gbc.insets = new Insets(0, 1, 0, 1);
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		add(actionLabel, gbc);

		// add textfield border to panel
		setBorder(new TextFieldBorder());
		// hide textfield border
		field.setBorder(null);
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		// paint background and then field background to simulate round border textfield
		if (isOpaque()) {
			g2.setColor(getBackground());
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
		g2.setColor(field.getBackground());
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);
	}

	@Override
	public boolean hasFocus() {
		return field.isFocusOwner();
	}

	/**
	 * Sets the icon which is painted if no text is in this textfield.
	 *
	 * @param defaultIcon
	 * 		the icon to paint if no text has been entered. If not specified, no icon will be painted
	 * @since 8.1
	 */
	public void setDefaultIcon(ImageIcon defaultIcon) {
		this.defaultIcon = defaultIcon;
	}

	/**
	 * Forces the given icon to be visible no matter how the user interacts with the text field. To unset, call again with {@code null}.
	 *
	 * @param forceIcon
	 * 		the icon which should always be painted regardless of state of the text field. Set to {@code null} to remove the forced icon
	 * @since 8.1
	 */
	public void setForceIcon(ImageIcon forceIcon) {
		this.forceIcon = forceIcon;
		if (forceIcon != null) {
			actionLabel.setIcon(forceIcon);
		} else {
			actionLabel.setIcon(actionIcon);
		}
	}

	/**
	 * Returns the actual text field.
	 *
	 * @return the field, never {@code null}
	 * @since 9.2.1
	 */
	public JTextField getField() {
		return field;
	}
}
