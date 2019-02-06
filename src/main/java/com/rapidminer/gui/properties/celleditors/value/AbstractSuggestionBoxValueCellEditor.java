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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.image.ImageObserver;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.autocomplete.AutoCompleteComboBoxAddition;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.config.Configurable;


/**
 * Renders a combo box which can be filled with suggestions.
 *
 * @author Marcin Skirzynski, Nils Woehler
 */
public abstract class AbstractSuggestionBoxValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -771727412083431607L;

	/** @since 9.2.0 */
	private static final Image LOADING_ICON = GrayFilter.createDisabledImage(SwingTools.createIcon("16/loading.gif").getImage());
	private static final String LOADING_STRING = I18N.getGUILabel("parameters.loading");
	private static final String EMPTY_STRING = I18N.getGUILabel("parameters.empty");
	private static final Object LOADING = new Object() {
		@Override
		public String toString() {
			return LOADING_STRING;
		}
	};
	private static final Object EMPTY = new Object() {
		@Override
		public String toString() {
			return EMPTY_STRING;
		}
	};

	/**
	 * The model of the combo box which consist of the suggestions
	 */
	private final SuggestionComboBoxModel model;

	/**
	 * The GUI element
	 */
	private final SuggestionComboBox comboBox;
	private final ImageIcon loadingIcon;
	private final JPanel container;

	private Operator operator;

	private ParameterType type;

	private ActionListener comboBoxSelectionListener = new ActionListener() {

		private Object currentItem = null;

		@Override
		public void actionPerformed(ActionEvent e) {
			Object selectedItem = comboBox.getSelectedItem();
			if (EMPTY == selectedItem || LOADING == selectedItem) {
				comboBox.setSelectedItem(currentItem == null ? "" : currentItem);
			} else {
				currentItem = selectedItem;
			}
		}
	};

	public AbstractSuggestionBoxValueCellEditor(final ParameterType type) {
		this.type = type;
		this.model = new SuggestionComboBoxModel();
		this.comboBox = new SuggestionComboBox(model);
		this.comboBox.setToolTipText(type.getDescription());
		this.comboBox.setRenderer(new SuggestionComboBoxModelCellRenderer());
		comboBox.addActionListener(comboBoxSelectionListener);

		loadingIcon = makeLoadingIcon(comboBox);

		new AutoCompleteComboBoxAddition(comboBox);

		this.container = new JPanel(new GridBagLayout());
		this.container.setToolTipText(type.getDescription());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		container.add(comboBox, c);
	}

	/**
	 * @return the parameter type of the suggestion box
	 */
	protected ParameterType getParameterType() {
		return type;
	}

	/**
	 * @return the combo box ui element displaying the selection
	 */
	protected SuggestionComboBox getSuggestionComboBox() {
		return comboBox;
	}

	/**
	 * Classes extending the {@link AbstractSuggestionBoxValueCellEditor} have to provide
	 * suggestions via this method. It is called whenever the combobox if opened.
	 *
	 * @param operator
	 *            the operator which is being configured. <b>CAUTION</b>: Can be <code>null</code>
	 *            if parameter type is used to e.g. configure {@link Configurable}s.
	 * @param progressListener
	 *            the progress listener to report the progress to
	 */
	public abstract List<Object> getSuggestions(Operator operator, ProgressListener progressListener);

	/**
	 * @return the current value. If there is an operator, the operator is asked, otherwise the
	 *         value from the combobox is taken.
	 */
	private String getValue() {
		if (operator == null) {
			return String.valueOf(comboBox.getEditor().getItem());
		}
		return operator.getParameters().getParameterOrNull(type.getKey());
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		comboBox.setSelectedItem(value);
		return container;
	}

	@Override
	public Object getCellEditorValue() {
		return comboBox.getEditor().getItem();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		comboBox.setSelectedItem(value);
		return container;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public class SuggestionComboBoxModelCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			Component listCellRendererComponent = super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			boolean isLoading = LOADING == value;
			if (EMPTY == value || isLoading) {
				listCellRendererComponent.setBackground(list.getBackground());
				listCellRendererComponent.setForeground(UIManager.getColor("Label.disabledForeground"));
				listCellRendererComponent.setEnabled(false);
				setDisabledIcon(isLoading ? loadingIcon : null);
				setText(value.toString());
			}
			return listCellRendererComponent;
		}

	}

	class SuggestionComboBoxModel extends DefaultComboBoxModel<Object> {

		private static final long serialVersionUID = -2984664300141879731L;

		private Object lock = new Object();

		public void updateModel() {
			fireEditingStopped();

			final Object selected = getValue();
			ProgressThread t = new ProgressThread("fetching_suggestions") {

				@Override
				public void run() {
					try {
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(0);

						synchronized (lock) {
							SwingTools.invokeAndWait(() -> {
								removeAllElements();

								insertElementAt(LOADING, 0);
								comboBox.getEditor().setItem(selected);
								makeComboBoxDropDownWider();
							});

							// fill list with stuff
							final List<Object> suggestions = getSuggestions(operator, getProgressListener());


							SwingTools.invokeAndWait(() -> {
								removeAllElements();
								if (suggestions.isEmpty()) {
									suggestions.add(EMPTY);
								}
								int index = 0;
								for (Object suggestion : suggestions) {
									insertElementAt(suggestion, index++);
								}
								comboBox.getEditor().setItem(selected);
								makeComboBoxDropDownWider();
							});
						}
					} finally {
						getProgressListener().complete();
					}
				}

			};
			t.addDependency("fetching_suggestions");
			t.start();
		}

		private void makeComboBoxDropDownWider() {
			Object child = comboBox.getAccessibleContext().getAccessibleChild(0);
			BasicComboPopup popup = (BasicComboPopup) child;
			Insets borderInsets = popup.getBorder().getBorderInsets(popup);
			JList<?> list = popup.getList();
			Dimension preferred = list.getPreferredSize();

			IntUnaryOperator extractWidth = i -> comboBox.getRenderer()
					.getListCellRendererComponent(list, model.getElementAt(i), i, false, false)
					.getPreferredSize().width + 3;
			preferred.width = IntStream.range(0, model.getSize()).map(extractWidth)
					.reduce(comboBox.getWidth() - borderInsets.left - borderInsets.right, Integer::max);

			int itemCount = comboBox.getItemCount();
			int rowHeight = 10;
			if (itemCount > 0) {
				rowHeight = preferred.height / itemCount;
			}
			int maxHeight = comboBox.getMaximumRowCount() * rowHeight;
			preferred.height = Math.min(preferred.height, maxHeight);

			Container c = SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
			JScrollPane scrollPane = (JScrollPane) c;

			scrollPane.setPreferredSize(preferred);
			scrollPane.setMaximumSize(preferred);

			if (popup.isShowing()) { // 
				// a little trick to fire Popup.showPopup method that takes care of correct display
				popup.setLocation(comboBox.getLocationOnScreen().x, popup.getLocationOnScreen().y - 1);
			}

			Dimension popupSize = new Dimension();
			popupSize.width = preferred.width + borderInsets.left + borderInsets.right;
			popupSize.height = preferred.height + borderInsets.top + borderInsets.bottom;
			Component parent = popup.getParent();
			if (parent != null) {
				parent.setSize(popupSize);
				parent.validate();
				parent.repaint();
			}
		}
	}

	class SuggestionComboBox extends JComboBox<Object> {

		private static final long serialVersionUID = 4000279412600950101L;

		private SuggestionComboBox(final SuggestionComboBoxModel model) {
			super(model);
			setEditable(true);
			addItemListener(e -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					fireEditingStopped();
				}
			});
			getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {

				@Override
				public void focusLost(FocusEvent e) {
					if (!e.isTemporary()) {
						fireEditingStopped();
					}
				}
			});

			// add popup menu listener
			Object child = getAccessibleContext().getAccessibleChild(0);
			BasicComboPopup popup = (BasicComboPopup) child;
			popup.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					model.updateModel();
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {}
			});
		}

	}

	/**
	 * @param button
	 *            adds a button the the right side of the ComboBox.
	 */
	protected void addConfigureButton(JButton button) {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 0;
		c.insets = new Insets(0, 5, 0, 0);
		container.add(button, c);
	}

	/**
	 * Creates a grayed out version of the loading gif that can be used in a combobox through an image observer
	 *
	 * @since 9.2.0
	 */
	private static ImageIcon makeLoadingIcon(JComboBox comboBox) {
		ImageIcon icon = new ImageIcon(LOADING_ICON);
		icon.setImageObserver((img, infoflags, x, y, w, h) -> {
			boolean isFrameOrAll = (infoflags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS)) != 0;
			if (!comboBox.isShowing() || !isFrameOrAll) {
				return !isFrameOrAll;
			}
			if (comboBox.getSelectedIndex() == 0) {
				comboBox.repaint();
			}
			BasicComboPopup p = (BasicComboPopup) comboBox.getAccessibleContext().getAccessibleChild(0);
			JList list = p.getList();
			if (list.isShowing()) {
				list.repaint();
			}
			return true;
		});
		return icon;
	}
}
