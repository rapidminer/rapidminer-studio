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

import java.awt.Graphics;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.composite.CompositeButtonPainter;
import com.rapidminer.tools.I18N;


/**
 * Class for a button with a JPopupMenu that acts as a dropdown menu. The popup menu opens when the
 * button is clicked and closes on another button click or when clicking somewhere else.
 *
 * @author Marco Boeck, Gisa Schaefer
 * @since 7.0.0
 */
public class DropDownPopupButton extends JButton {

	/**
	 * The default font size for arrow and text spans.
	 */
	private static final int DEFAULT_FONT_SIZE = 12;

	/**
	 * The default color used for text and arrow.
	 */
	private static final String DEFAULT_COLOR = "4F4F4F";

	/**
	 * at least this amount of ms have passed before the popup can be opened again.
	 */
	private static final int POPUP_CLOSE_DELTA = 250;

	/**
	 * Interface providing a method to obtain a {@link JPopupMenu}.
	 */
	public interface PopupMenuProvider {

		/**
		 * Returns a popup menu that should be used.
		 *
		 * @return a popup menu
		 */
		JPopupMenu getPopupMenu();
	}

	/**
	 * A builder for {@link DropDownPopupButton}s. The {@link #build()} method creates a button with
	 * text, icon and tooltip specified via {@link #with(ResourceAction)} that shows a popup menu
	 * with entries specified by {@link #add(Action)} and {@link #add(JMenuItem)}.
	 *
	 * @author Gisa Schaefer
	 *
	 */
	public static class DropDownPopupButtonBuilder {

		private ResourceAction action;

		private JPopupMenu popupMenu = new JPopupMenu();

		/** -1 means regular button, otherwise {@link com.rapidminer.gui.tools.components.composite.CompositeButton} */
		private int position = -1;

		/**
		 * Sets the action that specifies the text, icon and tooltip of the button created by this
		 * builder.
		 *
		 * @param action
		 *            the {@link ResourceAction} specifying the text, icon and tooltip of the button
		 * @return the builder
		 */
		public DropDownPopupButtonBuilder with(ResourceAction action) {
			this.action = action;
			return this;
		}

		/**
		 * Adds an action to the popup menu that is displayed when the button is clicked.
		 *
		 * @param action
		 *            the action to add
		 * @return the builder
		 */
		public DropDownPopupButtonBuilder add(Action action) {
			popupMenu.add(action);
			return this;
		}

		/**
		 * Adds an item to the popup menu that is displayed when the button is clicked.
		 *
		 * @param item
		 *            the item to add
		 * @return the builder
		 */
		public DropDownPopupButtonBuilder add(JMenuItem item) {
			popupMenu.add(item);
			return this;
		}

		/**
		 * Adds a separator to the popup menu that is displayed when the button is clicked.
		 *
		 * @return the builder
		 */
		public DropDownPopupButtonBuilder addSeparator() {
			popupMenu.addSeparator();
			return this;
		}

		/**
		 * Optional. If specified, the button will not be a standalone button, but rather be assumed to be part of multiple buttons
		 * directly next to each other, see {@link com.rapidminer.gui.tools.components.composite.CompositeButton}.
		 *
		 * @param position
		 * 		the position in the composite element ({@link SwingConstants#LEFT},
		 * 		{@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT})
		 * @return the builder
		 * @since 8.1
		 */
		public DropDownPopupButtonBuilder setComposite(int position) {
			this.position = position;
			return this;
		}

		/**
		 * Creates a {@link DropDownPopupButton} from the given data.
		 *
		 * @return the button created with the given data
		 */
		public DropDownPopupButton build() {
			DropDownPopupButton button =  new DropDownPopupButton(action, () -> popupMenu);
			button.setComposite(position);
			return button;
		}

	}

	private static final long serialVersionUID = -3267770082941303097L;

	private static final String DOWN_ARROW_ADDER_WITH_TEXT_AND_ARROW_SIZE = "<html><span style=\"color: %s; font-size:%d\">%s</span><span style=\"color: %s; font-size:%d\">"
			+ Ionicon.ARROW_DOWN_B.getHtml() + "</span></html>";

	/**
	 * hack to prevent popup from opening itself again when you click the button to actually close
	 * it while it is open
	 */
	private long lastPopupCloseTime;

	private final PopupMenuProvider menuProvider;

	private int position = -1;

	/** Paints the component background and border. */
	private CompositeButtonPainter painter;

	// small hack to prevent the popup from opening itself when you click
	// the button to actually close it
	private final PopupMenuListener popupListener = new PopupMenuListener() {

		@Override
		public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {}

		@Override
		public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
			lastPopupCloseTime = System.currentTimeMillis();
			JPopupMenu jPopupMenu = (JPopupMenu) e.getSource();
			jPopupMenu.removePopupMenuListener(this);

			for (PopupMenuListener otherListener : otherListeners) {
				jPopupMenu.removePopupMenuListener(otherListener);
			}

		}

		@Override
		public void popupMenuCanceled(final PopupMenuEvent e) {}
	};

	private List<PopupMenuListener> otherListeners = new LinkedList<>();

	private int arrowSize = DEFAULT_FONT_SIZE;
	private int fontSize = DEFAULT_FONT_SIZE;
	private String color = DEFAULT_COLOR;

	private String text;

	/**
	 * Creates a button with text, icon and tooltip specified by the i18n together with a popup menu
	 * that acts as a dropdown.
	 *
	 * @param i18n
	 *            [i18n].label for the text of the button, [i18n].icon for the icon and [i18n].tip
	 *            for the tooltip
	 * @param popupMenuProvider
	 *            the provider for the menu that is shown when the button is clicked
	 */
	public DropDownPopupButton(String i18n, PopupMenuProvider popupMenuProvider) {
		super(I18N.getGUIMessageOrNull(i18n + ".label"), getIcon(i18n));
		setToolTipText(I18N.getGUIMessageOrNull(i18n + ".tip"));

		menuProvider = popupMenuProvider;

		setupAction();
	}

	/**
	 * Creates a button with text, icon and tooltip specified by the action together with a popup
	 * menu that acts as a dropdown.
	 *
	 * @param action
	 *            the {@link ResourceAction} that provides the text, icon and tooltip for the button
	 * @param popupMenuProvider
	 *            the provider for the menu that is shown when the button is clicked
	 */
	public DropDownPopupButton(ResourceAction action, PopupMenuProvider popupMenuProvider) {
		super(action);
		menuProvider = popupMenuProvider;
		setupAction();
	}

	/**
	 * Makes this a composite button, i.e. one that is part of other buttons right next to each other with no space in between.
	 *
	 * @param position
	 * 		a value of {@code -1} means regular button, otherwise see {@link com.rapidminer.gui.tools.components.composite.CompositeButton}
	 */
	private void setComposite(int position) {
		this.position = position;
		if (position !=-1) {
			super.setContentAreaFilled(false);
			super.setBorderPainted(false);
			painter = new CompositeButtonPainter(this, position);
		} else {
			super.setContentAreaFilled(true);
			super.setBorderPainted(true);
			painter = null;
		}
	}

	private void setupAction() {
		addActionListener(e -> {
			JPopupMenu popupMenu = menuProvider.getPopupMenu();

			if (!popupMenu.isVisible()) {
				// hack to prevent filter popup from opening itself again
				// when you click the button to actually close it while it
				// is open
				if (System.currentTimeMillis() - lastPopupCloseTime < POPUP_CLOSE_DELTA) {
					return;
				}
				popupMenu.addPopupMenuListener(popupListener);
				for (PopupMenuListener listener : otherListeners) {
					popupMenu.addPopupMenuListener(listener);
				}

				popupMenu.show(DropDownPopupButton.this, 0, DropDownPopupButton.this.getHeight() - 1);
				popupMenu.requestFocusInWindow();
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (position != -1) {
			painter.paintComponent(g);
		}

		// still super.paint because otherwise text is not painted etc.
		super.paintComponent(g);
	}

	@Override
	protected void paintBorder(Graphics g) {
		if (position != -1) {
			painter.paintBorder(g);
		} else {
			super.paintBorder(g);
		}
	}

	@Override
	public void setText(String text) {
		this.text = text;
		if (text == null) {
			text = "";
		}
		if (!text.isEmpty()) {
			// add space between text and arrow
			text = text + "&#160 ";
		}

		int arrowFontSize = getFontSize(arrowSize);
		int textFontSize = getFontSize(fontSize);
		String color = getColor();

		super.setText(
				String.format(DOWN_ARROW_ADDER_WITH_TEXT_AND_ARROW_SIZE, color, textFontSize, text, color, arrowFontSize));
	}

	private int getFontSize(int selectedSize) {
		if (selectedSize <= 0) {
			return DEFAULT_FONT_SIZE;
		}
		return selectedSize;
	}

	/**
	 * Sets the arrow size. The default size is 12.
	 *
	 * @param size
	 *            the size of the downward pointing arrow
	 */
	public void setArrowSize(int size) {
		arrowSize = size;
		setText(text);
	}

	/**
	 * Sets the text font size. The default size is 12.
	 *
	 * @param size
	 *            the size of the text
	 */
	public void setTextSize(int size) {
		fontSize = size;
		setText(text);
	}

	/**
	 * Sets the color for the font and arrow (in hex format). If set to {@code null} the default
	 * color {@link #DEFAULT_COLOR} is used.
	 *
	 * @param color
	 *            the color (in hex format) that should be used for font and arrow
	 */
	public void setColor(String color) {
		this.color = color;
		setText(text);
	}

	private String getColor() {
		if (color == null) {
			return DEFAULT_COLOR;
		}
		return color;
	}

	/**
	 * Creates image icon from the i18n; static since called from within the constructor.
	 */
	private static ImageIcon getIcon(String i18n) {
		String iconName = I18N.getGUIMessageOrNull(i18n + ".icon");
		if (iconName == null) {
			return null;
		}
		return SwingTools.createIcon("16/" + iconName);
	}

	/**
	 * Adds a {@link PopupMenuListener} that will be registered to the buttons popup menu each time
	 * it is shown.
	 *
	 * @param listener
	 *            the listener to add
	 */
	public void addPopupMenuListener(PopupMenuListener listener) {
		otherListeners.add(listener);
	}
}
