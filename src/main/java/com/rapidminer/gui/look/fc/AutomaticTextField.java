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
package com.rapidminer.gui.look.fc;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;


/**
 * This text field is connected to the file chooser and provides some automation mechanisms.
 * 
 * @author Ingo Mierswa
 */
public class AutomaticTextField extends JTextField implements KeyListener, CaretListener, InputMethodListener {

	private static final long serialVersionUID = 9138792080755085206L;

	private FileChooserUI fileChooserUI;

	private JList<String> itemList = new JList<>(new String[] {});

	private JPopupMenu popupMenu = new JPopupMenu();

	private Vector<String> fileList = new Vector<String>();

	private int caretPosition = 0;

	private String mainText;

	public AutomaticTextField(FileChooserUI fcui) {
		super();
		this.fileChooserUI = fcui;
		this.addCaretListener(this);
		this.addKeyListener(this);
	}

	private Vector<String> findItems() {
		if (this.mainText == null) {
			this.mainText = this.getText();
		}

		this.fileList.removeAllElements();

		int minPos = Math.min(this.caretPosition, this.mainText.length());
		for (Item vi : fileChooserUI.fileList.visibleItemsList) {
			if (vi.getFileName().toLowerCase().startsWith(this.mainText.toLowerCase().substring(0, minPos))) {

				this.fileList.add(vi.getFileName());
			}
		}
		return this.fileList;
	}

	private void generatePopup(boolean help) {
		this.popupMenu.setVisible(false);
		this.popupMenu.removeAll();
		final JTextField t = this;

		this.popupMenu.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().toLowerCase().equals("visible")) {
					if (e.getNewValue().toString().toLowerCase().equals("false")) {
						AutomaticTextField.this.mainText = t.getText();
					}
				}
			}
		});

		this.itemList = new JList<>(findItems());
		this.itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.itemList.setBorder(null);
		this.itemList.setAutoscrolls(true);

		this.itemList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				AutomaticTextField.this.popupMenu.setVisible(false);
				t.setSelectionEnd(0);
				t.setCaretPosition(t.getText().length());
			}
		});

		this.itemList.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					AutomaticTextField.this.popupMenu.setVisible(false);
					t.setSelectionEnd(0);
					t.setText(AutomaticTextField.this.mainText);
					t.setCaretPosition(t.getText().length());
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					AutomaticTextField.this.popupMenu.setVisible(false);
					t.setSelectionEnd(0);
					t.setCaretPosition(t.getText().length());
				}
			}
		});

		this.itemList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				AutomaticTextField.this.itemList.scrollRectToVisible(AutomaticTextField.this.itemList.getCellBounds(
						AutomaticTextField.this.itemList.getSelectedIndex(),
						AutomaticTextField.this.itemList.getSelectedIndex()));

				String cpt = AutomaticTextField.this.mainText;
				try {
					cpt = t.getText(AutomaticTextField.this.caretPosition, t.getText().length()
							- AutomaticTextField.this.caretPosition);
				} catch (BadLocationException ex) {
				}

				if (!AutomaticTextField.this.itemList.getModel()
						.getElementAt(AutomaticTextField.this.itemList.getSelectedIndex()).toString().equals(cpt)) {
					t.setText(AutomaticTextField.this.itemList.getModel()
							.getElementAt(AutomaticTextField.this.itemList.getSelectedIndex()).toString());
					t.setCaretPosition(AutomaticTextField.this.caretPosition);

					if (AutomaticTextField.this.caretPosition == 0
							|| !t.getText().toLowerCase().startsWith(AutomaticTextField.this.mainText.toLowerCase())) {
						t.select(0, t.getText().length());
					} else {
						t.select(AutomaticTextField.this.mainText.length(), t.getText().length());
					}
				}
			}
		});

		JScrollPane scroller = new JScrollPane(this.itemList);
		scroller.setBorder(null);
		scroller.setFocusable(false);
		this.popupMenu.add(scroller);

		int h = 30;
		try {
			if (this.itemList.getModel().getSize() > 0) {
				h = (int) (this.itemList.getModel().getSize() * this.itemList.getCellBounds(0, 0).getHeight()) + 10;
			}
		} catch (Exception exp) {
		}

		this.popupMenu.setPopupSize(this.getWidth() - 6, Math.min(100, h));

		if (this.itemList.getModel().getSize() > 0) {
			this.popupMenu.show(this, 3, this.getHeight() - 2);

			if (help) {
				this.itemList.getSelectionModel().setSelectionInterval(0, 0);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			this.popupMenu.setVisible(false);
			this.setSelectionEnd(0);
			this.setCaretPosition(this.getText().length());
			this.mainText = this.getText();
		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			if (this.popupMenu.isVisible()) {
				this.setText(this.mainText);
				this.popupMenu.setVisible(false);
				this.setSelectionEnd(0);
				this.setCaretPosition(this.getText().length());
				e.consume();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			this.caretPosition = this.getCaretPosition();
			generatePopup(false);
			this.requestFocus();
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			this.caretPosition = this.getCaretPosition();
			generatePopup(false);
			if (!this.popupMenu.isVisible()) {
				this.setText(this.mainText);
				this.setCaretPosition(this.caretPosition);
			}
			this.requestFocus();
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			if (!this.popupMenu.isVisible()) {
				generatePopup(true);
			}
			int n = this.itemList.getSelectedIndex();
			n -= 1;
			if (n < 0) {
				n = 0;
			}
			this.itemList.setSelectedIndex(n);
			this.requestFocus();
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			if (!this.popupMenu.isVisible()) {
				generatePopup(true);
			}
			int n = this.itemList.getSelectedIndex();
			n += 1;
			if (n >= this.itemList.getModel().getSize()) {
				n = this.itemList.getModel().getSize() - 1;
			}
			this.itemList.setSelectedIndex(n);
			this.requestFocus();
		} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
			e.consume();
			this.mainText = this.getText();
			this.requestFocus();
		}
		if (e.isActionKey() && this.popupMenu.isVisible()) {
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			e.consume();
		} else if (!e.isActionKey()) {
			this.mainText = this.getText();
			generatePopup(true);
			this.requestFocus();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			e.consume();
		}
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		this.caretPosition = e.getDot();
	}

	public void propertyChange(PropertyChangeEvent evt) {}

	@Override
	public void caretPositionChanged(InputMethodEvent event) {}

	@Override
	public void inputMethodTextChanged(InputMethodEvent event) {}
}
