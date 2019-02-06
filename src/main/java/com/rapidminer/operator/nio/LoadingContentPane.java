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
package com.rapidminer.operator.nio;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadListener;
import com.rapidminer.gui.tools.ResourceLabel;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;


/**
 * This class is a swing component which shows a loading panel when you call initLoading() and is
 * able to switch to the loaded content when you call finishedLoading()
 * 
 * @author Dominik Halfkann
 */
public class LoadingContentPane extends JPanel implements ProgressThreadListener {

	private static final long serialVersionUID = 1L;
	private String label;
	private boolean loading = false;

	public LoadingContentPane(String label, JComponent content) {
		this.label = label;
		setLayout(new CardLayout());
		add(createDummyPanel());
		add(content);
	}

	private JPanel createDummyPanel() {
		JPanel dummy = new JPanel();
		dummy.add(new ResourceLabel(label));
		return dummy;
	}

	/** Initializes the LoadingContentPane with a loading screen until loadingFinished() is called. **/
	public void init() {
		((CardLayout) (getLayout())).first(this);
		loading = true;
	}

	/**
	 * Initializes the LoadingContentPane with a loading screen and waits for the ProgressThread to
	 * be finished.
	 **/
	public void init(ProgressThread thread) {
		((CardLayout) (getLayout())).first(this);
		loading = true;
		thread.addProgressThreadListener(this);
	}

	/**
	 * Tells the LoadingContentPane that the content has finished loading, switches to the actual
	 * content pane.
	 **/
	public void loadingFinished() {
		if (loading) {
			((CardLayout) (getLayout())).last(LoadingContentPane.this);
			loading = false;
		}
	}

	/**
	 * This method should only be called from the ProgressThread when it's finished. Causes the
	 * LoadingContentPane to switch to the actual content pane.
	 **/
	@Override
	public void threadFinished(ProgressThread thread) {
		if (loading) {
			((CardLayout) (getLayout())).last(LoadingContentPane.this);
			loading = false;
		}
		thread.removeProgressThreadListener(this);
	}

}
