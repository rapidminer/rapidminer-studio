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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * Creates a panel which contains a {@link JTextField} and a {@link JLabel} which can be used to
 * enter the name of a repository entry. Invalid names (checked via
 * {@link RepositoryLocation#isNameValid(String)}) are marked invalid while typing and an event is
 * fired to signal invalid/valid entries.
 * <p>
 * To listen to the event (firing a simple {@link Boolean} to signal valid (<code>true</code>) and
 * invalid (<code>false</code>) entries), register your listener via
 * {@link #addObserver(Observer, boolean)} or {@link #addObserverAsFirst(Observer, boolean)}.
 * 
 * @author Marco Boeck
 */
public class RepositoryEntryTextField extends JPanel implements Observable<Boolean> {

	private static final long serialVersionUID = -750857028654448541L;

	private JLabel entryTextLabel;
	private JTextField entryTextField;

	private JLabel entryErrorIconLabel;
	private JLabel entryErrorTextLabel;

	private final Color standardTextColor;
	private final Color errorTextColor;

	private final Icon standardIcon;
	private final Icon errorIcon;

	private List<Observer<Boolean>> observerOnEDTList;
	private List<Observer<Boolean>> observerNotOnEDTList;

	private Object lock = new Object();

	/**
	 * Standard constructor.
	 */
	public RepositoryEntryTextField() {
		super();

		standardIcon = null;
		errorIcon = SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.repository_location.location_invalid.icon"));

		setupGUI();

		standardTextColor = entryTextField.getForeground();
		errorTextColor = Color.RED;

		observerOnEDTList = new LinkedList<Observer<Boolean>>();
		observerNotOnEDTList = new LinkedList<Observer<Boolean>>();

		checkName();
	}

	/**
	 * Sets up this GUI element.
	 */
	private void setupGUI() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(5, 0, 5, 5);
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 1;
		entryTextField = new JTextField();
		entryTextLabel = new ResourceLabel("repository_location.location_entry_name");
		entryTextLabel.setLabelFor(entryTextField);
		add(entryTextLabel, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.insets = new Insets(5, 0, 5, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = 2;
		entryTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				checkName();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				checkName();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// not relevant
			}
		});
		add(entryTextField, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(5, 0, 5, 5);
		entryErrorIconLabel = new JLabel();
		entryErrorIconLabel.setMinimumSize(new Dimension(16, 16));
		entryErrorIconLabel.setPreferredSize(new Dimension(16, 16));
		add(entryErrorIconLabel, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(5, 0, 5, 0);
		entryErrorTextLabel = new JLabel();
		add(entryErrorTextLabel, gbc);
	}

	/**
	 * Checks if the entered repository entry name is valid.
	 */
	private void checkName() {
		boolean valid = false;
		boolean empty = false;
		String illegalSubstring = null;
		try {
			String name = entryTextField.getDocument().getText(0, entryTextField.getDocument().getLength());
			if ("".equals(name.trim())) {
				empty = true;
			}
			valid = RepositoryLocation.isNameValid(name);
			if (!valid) {
				illegalSubstring = RepositoryLocation.getIllegalCharacterInName(name);
			}
		} catch (BadLocationException e1) {
			LogService.getRoot().log(
					Level.SEVERE,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.tools.RepositoryEntryTextField.bad_document_location"), e1);
		}

		if (!valid) {
			entryTextField.setForeground(errorTextColor);
			entryErrorIconLabel.setIcon(errorIcon);
			if (!empty) {
				entryErrorTextLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.repository_location.location_invalid_char.label", illegalSubstring));
			} else {
				entryErrorTextLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.repository_location.location_invalid_empty.label"));
			}
		} else {
			entryTextField.setForeground(standardTextColor);
			entryErrorIconLabel.setIcon(standardIcon);
			entryErrorTextLabel.setText("");
		}

		notifyObservers(valid);
	}

	/**
	 * Notifies the observers.
	 * 
	 * @param valid
	 */
	private void notifyObservers(final boolean valid) {
		synchronized (lock) {
			if (SwingUtilities.isEventDispatchThread()) {
				for (final Observer<Boolean> observer : observerOnEDTList) {
					observer.update(RepositoryEntryTextField.this, valid);
				}
			} else {
				for (final Observer<Boolean> observer : observerOnEDTList) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							observer.update(RepositoryEntryTextField.this, valid);
						}

					});
				}
			}

			for (final Observer<Boolean> observer : observerNotOnEDTList) {
				observer.update(RepositoryEntryTextField.this, valid);
			}
		}
	}

	@Override
	public void addObserver(Observer<Boolean> observer, boolean onEDT) {
		if (onEDT) {
			synchronized (lock) {
				observerOnEDTList.add(observer);
			}
		} else {
			synchronized (lock) {
				observerNotOnEDTList.add(observer);
			}
		}
	}

	@Override
	public void removeObserver(Observer<Boolean> observer) {
		synchronized (lock) {
			if (!observerOnEDTList.remove(observer)) {
				observerNotOnEDTList.remove(observer);
			}
		}
	}

	@Override
	public void addObserverAsFirst(Observer<Boolean> observer, boolean onEDT) {
		if (onEDT) {
			synchronized (lock) {
				observerOnEDTList.add(0, observer);
			}
		} else {
			synchronized (lock) {
				observerNotOnEDTList.add(0, observer);
			}
		}
	}

	/**
	 * Sets a text in the textfield.
	 * 
	 * @param text
	 */
	public void setText(String text) {
		entryTextField.setText(text);
	}

	/**
	 * Return the entered text.
	 * 
	 * @return
	 */
	public String getText() {
		return entryTextField.getText();
	}

	/**
	 * Triggers a new check if the entered name is valid. Will notify all {@link Observer}s!
	 */
	public void triggerCheck() {
		checkName();
	}

	@Override
	public void addKeyListener(KeyListener l) {
		this.entryTextField.addKeyListener(l);
	}

	@Override
	public void removeKeyListener(KeyListener l) {
		this.entryTextField.removeKeyListener(l);
	}

	@Override
	public boolean requestFocusInWindow() {
		return entryTextField.requestFocusInWindow();
	}
}
