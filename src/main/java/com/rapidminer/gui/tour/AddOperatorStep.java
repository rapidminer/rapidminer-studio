/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
package com.rapidminer.gui.tour;

import java.awt.Window;

import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.internal.ProcessEmbeddingOperator;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.nio.file.LoadFileOperator;
import com.rapidminer.repository.gui.RepositoryBrowser;


/**
 * This Subclass of {@link Step} will open a {@link BubbleWindow} which closes if the given
 * {@link Operator} was dragged to the Process and is wired.
 *
 * @author Philipp Kersting and Thilo Kamradt
 *
 */

public class AddOperatorStep extends Step {

	private String i18nKey;
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private Class<?> operatorClass;
	private boolean isRepository;
	private String repositoryBrowserKey = RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY;
	private String operatorEditorKey = NewOperatorEditor.NEW_OPERATOR_DOCK_KEY;
	private boolean checkForChain = true;
	private Class<? extends OperatorChain> targetEnclosingOperatorChain = OperatorChain.class;
	private ProcessSetupListener listener = null;

	/**
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass
	 *            the Class or Superclass of the Operator which should be added to the Process.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public AddOperatorStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass,
			Object... arguments) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.checkClassInput();
		this.arguments = arguments;
	}

	/**
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass
	 *            the Class or Superclass of the Operator which should be added to the Process.
	 * @param checkForEnclosingOperatorChain
	 *            indicates whether the {@link BubbleWindow} closes only if the Operator is also
	 *            wired or not
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public AddOperatorStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass,
			boolean checkForEnclosingOperatorChain, Object... arguments) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.checkForChain = checkForEnclosingOperatorChain;
		this.checkClassInput();
		this.arguments = arguments;
	}

	/**
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param operatorClass
	 *            the Class or Superclass of the Operator which should be added to the Process.
	 * @param targetEnclosingOperatorChain
	 *            target OperatorChain
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public AddOperatorStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass,
			Class<? extends OperatorChain> targetEnclosingOperatorChain, Object... arguments) {
		this.i18nKey = i18nKey;
		this.alignment = preferredAlignment;
		this.operatorClass = operatorClass;
		this.targetEnclosingOperatorChain = targetEnclosingOperatorChain;
		this.arguments = arguments;
		this.checkClassInput();
	}

	/**
	 * checks whether the given Class is an Operator-Class or a Repository-Class and writes the
	 * result in the isRepository-variable
	 */
	private void checkClassInput() {
		this.isRepository = RepositorySource.class.isAssignableFrom(operatorClass)
				|| ProcessEmbeddingOperator.class.isAssignableFrom(operatorClass)
				|| LoadFileOperator.class.isAssignableFrom(operatorClass);
	}

	@Override
	boolean createBubble() {
		bubble = new DockableBubble(owner, alignment, i18nKey, this.isRepository ? repositoryBrowserKey : operatorEditorKey,
				arguments);
		listener = new ProcessSetupListener() {

			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {}

			@Override
			public void operatorChanged(Operator operator) {
				if (operatorClass.isInstance(operator)) {
					if (checkForChain) {
						if (operator.getExecutionUnit() != null
								&& (targetEnclosingOperatorChain == null || targetEnclosingOperatorChain.isInstance(operator
										.getExecutionUnit().getEnclosingOperator()))
								&& operator.getOutputPorts().getNumberOfConnectedPorts() != 0) {

							bubble.triggerFire();
							RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
						}
					} else {
						if (operator.getOutputPorts().getNumberOfConnectedPorts() != 0) {

							bubble.triggerFire();
							RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
						}
					}
				}
			}

			@Override
			public void operatorAdded(Operator operator) {}

			@Override
			public void executionOrderChanged(ExecutionUnit unit) {}
		};
		RapidMinerGUI.getMainFrame().getProcess().addProcessSetupListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null) {
			RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(listener);
		}
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] { new PerspectivesStep(1),
				new NotShowingStep(this.isRepository ? repositoryBrowserKey : operatorEditorKey),
				new NotShowingStep(ProcessPanel.PROCESS_PANEL_DOCK_KEY) };
	}

}
