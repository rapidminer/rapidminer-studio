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
package com.rapidminer.gui.tools.dialogs;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.tools.ResourceAction;


/**
 *
 * @author Tobias Malbrecht
 */
public abstract class MultiPageDialog extends ButtonDialog implements ChangeListener {

	private static final long serialVersionUID = 1L;

	private int currentStep;

	private JPanel cardPanel;

	protected final JButton previous = new JButton(new ResourceAction("previous") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(java.awt.event.ActionEvent e) {
			previous();
		}
	});
	protected final JButton next = new JButton(new ResourceAction("next") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(java.awt.event.ActionEvent e) {
			next();
		}
	});
	protected final JButton finish = new JButton(new ResourceAction("finish") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(java.awt.event.ActionEvent e) {
			finish();
		}
	});

	public MultiPageDialog(Dialog owner, String key, boolean modal, Object... arguments) {
		super(owner, key, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS, arguments);
	}

	public MultiPageDialog(Dialog owner, String key, Object... arguments) {
		super(owner, key, ModalityType.MODELESS, arguments);
	}

	public MultiPageDialog(Frame owner, String key, boolean modal, Object... arguments) {
		super(owner, key, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS, arguments);
	}

	public MultiPageDialog(Frame owner, String key, Object... arguments) {
		super(owner, key, ModalityType.MODELESS, arguments);
	}

	protected abstract boolean isComplete();

	protected abstract boolean isLastStep(int step);

	protected abstract String getNameForStep(int step);

	protected boolean canProceed(int step) {
		return true;
	}

	protected boolean canGoBack(int step) {
		return true;
	}

	protected void layoutDefault(Map<String, Component> cards) {
		layoutDefault(cards, DEFAULT_SIZE);
	}

	/**
	 * @param cards
	 *            Maps arbitrary names to GUI cards. The name for the current component is looked up
	 *            by using {@link #getNameForStep(int)} as a key into this map. This is useful, if
	 *            we do not have a linear order of cards.
	 */
	protected void layoutDefault(Map<String, Component> cards, int size) {
		cardPanel = new JPanel(new CardLayout());
		for (Map.Entry<String, Component> entry : cards.entrySet()) {
			cardPanel.add(entry.getValue(), entry.getKey());
		}
		showCurrent();
		super.layoutDefault(cardPanel, size, previous, next, finish, makeCancelButton());
	}

	protected int getCurrentStep() {
		return currentStep;
	}

	private void showCurrent() {
		updateButtons();

		if (isLastStep(currentStep)) {
			getRootPane().setDefaultButton(finish);
		} else {
			getRootPane().setDefaultButton(next);
		}

		String key = getNameForStep(getCurrentStep());
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, key);
	}

	protected void finish() {
		wasConfirmed = true;
		dispose();
	}

	protected void previous() {
		currentStep--;
		showCurrent();
	}

	protected void next() {
		currentStep++;
		showCurrent();
	}

	private void updateButtons() {
		previous.setEnabled(currentStep > 0 && canGoBack(currentStep));
		next.setEnabled(!isLastStep(currentStep) && canProceed(currentStep));
		finish.setEnabled(isComplete() && canProceed(currentStep));
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		updateButtons();
	}

	protected JButton getFinishButton() {
		return finish;
	}
}
