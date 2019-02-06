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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * A dialog for bookmarks.
 * 
 * @author Ingo Mierswa
 */
public class BookmarkDialog extends JDialog {

	private static final long serialVersionUID = -5259373575470177110L;

	private class CancelAdapter implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			BookmarkDialog.this.nameChanged = false;
			dispose();
		}
	}

	private class OkAdapter implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			BookmarkDialog.this.nameChanged = true;
			dispose();
		}
	}

	private class EnterAdapter implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			BookmarkDialog.this.nameChanged = true;
			dispose();
		}
	}

	private class EscapeAdapter extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				BookmarkDialog.this.nameChanged = false;
				dispose();
			}
		}
	}

	private JPanel panel = new JPanel();

	private JTextField nameTextField = new JTextField();

	private JTextField pathTextField = new JTextField();

	private JButton okButton = new JButton();

	private JButton cancelButton = new JButton();

	private boolean nameChanged;

	public BookmarkDialog(Dialog top, boolean modal) {
		super(top, modal);
		try {
			init();
		} catch (Exception ex) {
		}
	}

	public BookmarkDialog(Frame top, boolean modal) {
		super(top, modal);
		try {
			init();
		} catch (Exception ex) {
		}
	}

	public boolean isNameChanged() {
		return this.nameChanged;
	}

	private void init() throws Exception {
		this.setTitle("Update Details");
		this.setSize(350, 180);
		this.panel.setLayout(null);
		JLabel label = new JLabel("Folder Path :");
		label.setBounds(new Rectangle(10, 60, 69, 20));
		this.nameTextField.setBounds(new Rectangle(95, 20, 230, 20));
		this.nameTextField.addKeyListener(new EscapeAdapter());
		this.nameTextField.addActionListener(new EnterAdapter());
		JLabel label2 = new JLabel("Name : ");
		label2.setBounds(new Rectangle(10, 20, 69, 20));
		this.pathTextField.setEditable(false);
		this.pathTextField.setBounds(new Rectangle(95, 60, 230, 20));
		this.okButton.setBounds(new Rectangle(145, 110, 80, 22));
		this.okButton.setText("OK");
		this.okButton.addActionListener(new OkAdapter());
		this.cancelButton.setBounds(new Rectangle(245, 110, 80, 22));
		this.cancelButton.setText("Cancel");
		this.cancelButton.addActionListener(new CancelAdapter());
		this.panel.add(this.nameTextField, null);
		this.panel.add(this.pathTextField, null);
		this.panel.add(this.cancelButton, null);
		this.panel.add(label, null);
		this.panel.add(label, null);
		this.panel.add(this.okButton, null);
		this.getContentPane().add(this.panel, BorderLayout.CENTER);
	}

	public void updateDefaults(String name, String path) {
		this.nameTextField.setText(name);
		this.pathTextField.setText(path);
	}

	public String getNewName() {
		return this.nameTextField.getText();
	}
}
