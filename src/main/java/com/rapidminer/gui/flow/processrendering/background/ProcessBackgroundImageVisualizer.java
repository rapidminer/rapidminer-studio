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
package com.rapidminer.gui.flow.processrendering.background;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;

import javax.swing.JPanel;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * This class manages the process background background image which can be added/edited in the
 * process context editor.
 *
 * @author Marco Boeck
 * @since 7.0.0
 *
 */
public final class ProcessBackgroundImageVisualizer {

	/** the process renderer */
	private final ProcessRendererView view;

	/** the draw decorator */
	private final ProcessBackgroundImageDecorator decorator;

	/**
	 * Creates the visualizer for {@link ProcessBackgroundImage}s.
	 *
	 * @param view
	 *            the proces renderer instance
	 */
	public ProcessBackgroundImageVisualizer(final ProcessRendererView view) {
		this.view = view;
		this.decorator = new ProcessBackgroundImageDecorator(view);

		// start background image decorators
		decorator.registerDecorators();
	}

	/**
	 * Creates an action which can be used to set the {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 *            the process for which to set the background image. Can be {@code null} for first
	 *            process at action event time
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeSetBackgroundImageAction(final ExecutionUnit process) {
		ResourceAction setBackgroundImage = new ResourceAction(true, "process_background.set") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				final ExecutionUnit targetProcess = process != null ? process : view.getModel().getProcess(0);

				ButtonDialog dialog = createBackgroundImageDialog(targetProcess, view.getModel());
				dialog.setVisible(true);
			}

		};

		return setBackgroundImage;
	}

	/**
	 * Creates an action which can be used to remove the {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 *            the process for which to remove the background image. Can be {@code null} for
	 *            first process at action event time
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeRemoveBackgroundImageAction(final ExecutionUnit process) {
		ResourceAction removeBackgroundImage = new ResourceAction(true, "process_background.remove") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {

				ExecutionUnit targetProcess = process;
				if (process == null) {
					targetProcess = view.getModel().getProcess(0);
				}

				view.getModel().removeBackgroundImage(targetProcess);
				view.getModel().fireMiscChanged();
				// dirty hack to trigger a process update
				targetProcess.getEnclosingOperator().rename(targetProcess.getEnclosingOperator().getName());
			}
		};

		return removeBackgroundImage;
	}

	/**
	 * Creates a dialog to set the background image of the given process.
	 * 
	 * @param process
	 *            the process for which the background image should be set
	 * @param model
	 *            the process renderer model instance
	 * @return the dialog
	 */
	private static ButtonDialog createBackgroundImageDialog(final ExecutionUnit process, final ProcessRendererModel model) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		if (model == null) {
			throw new IllegalArgumentException("model must not be null!");
		}

		final JPanel mainPanel = new JPanel(new BorderLayout());

		ProcessBackgroundImage prevImg = model.getBackgroundImage(process);
		final RepositoryLocationChooser chooser = new RepositoryLocationChooser(null, null,
				prevImg != null ? prevImg.getLocation() : "");
		mainPanel.add(chooser, BorderLayout.CENTER);

		ButtonDialog dialog = new ButtonDialog(ApplicationFrame.getApplicationFrame(), "process_background",
				ModalityType.DOCUMENT_MODAL, new Object[] {}) {

			private static final long serialVersionUID = 1L;

			{
				layoutDefault(mainPanel, NORMAL, makeOkButton(), makeCancelButton());
			}

			@Override
			protected void ok() {
				ProcessBackgroundImage img = null;
				try {
					img = new ProcessBackgroundImage(-1, -1, -1, -1, chooser.getRepositoryLocation(), process);
				} catch (MalformedRepositoryLocationException e) {
					// ignore
				}

				if (img != null) {
					model.setBackgroundImage(img);
					model.fireMiscChanged();

					// dirty hack to trigger a process update
					process.getEnclosingOperator().rename(process.getEnclosingOperator().getName());
				}

				super.ok();
			}
		};
		return dialog;
	}

}
