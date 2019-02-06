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
package com.rapidminer.operator.ports.impl;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOMultiplier;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


/**
 * 
 * @author Simon Fischer, Tobias Malbrecht, Nils Woehler
 */
public class CannotConnectPortException extends PortException {

	public static final int HIDE_DELAY = 2500;

	private static final long serialVersionUID = 5242982041478562116L;

	private final OutputPort source;
	private final InputPort dest;

	public CannotConnectPortException(OutputPort source, InputPort dest, InputPort sourceDest, OutputPort destSource) {
		super("Cannot connect " + source.getSpec() + " to " + dest.getSpec());
		this.source = source;
		this.dest = dest;
	}

	public CannotConnectPortException(OutputPort source, InputPort dest, InputPort sourceDest) {
		super("Cannot connect " + source.getSpec() + " to " + dest.getSpec());
		this.source = source;
		this.dest = dest;
	}

	public CannotConnectPortException(OutputPort source, InputPort dest, OutputPort destSource) {
		super("Cannot connect " + source.getSpec() + " to " + dest.getSpec());
		this.source = source;
		this.dest = dest;
	}

	@Override
	public boolean hasRepairOptions() {
		return true;
	}

	@Override
	public void showRepairPopup(Component parent, Point popupLocation) {

		// remember initial state first
		final boolean sourceConnected = source.isConnected();
		final InputPort oldDest = source.getDestination();

		// connect ports in any case
		source.lock();
		dest.lock();
		if (sourceConnected) {
			source.disconnect();
		}
		if (dest.isConnected()) {
			dest.getSource().disconnect();
		}
		source.connectTo(dest);
		source.unlock();
		dest.unlock();

		if (sourceConnected) {
			final JPopupMenu menu = new JPopupMenu();

			// give the user the possibility to create IO multiplier instead
			Action addIOMultiplierActions = new ResourceAction("cannot_connect.option.insert_multiplier") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					final List<Port> toUnlock = new LinkedList<Port>();
					try {

						// lock old dest port
						toUnlock.add(oldDest);
						oldDest.lock();

						// lock source port
						toUnlock.add(source);
						source.lock();

						// lock dest port
						toUnlock.add(dest);
						dest.lock();

						// disconnect source
						source.disconnect();

						// create IO multiplier
						IOMultiplier multiplier = OperatorService.createOperator(IOMultiplier.class);

						// connect source to multiplier
						source.getPorts().getOwner().getConnectionContext().addOperator(multiplier);
						source.connectTo(multiplier.getInputPorts().getPortByIndex(0));

						// connect multiplier to old dest and new dest
						multiplier.getOutputPorts().getPortByIndex(0).connectTo(oldDest);
						multiplier.getOutputPorts().getPortByIndex(1).connectTo(dest);
					} catch (OperatorCreationException e2) {
						LogService.getRoot().log(Level.WARNING, "Cannot create multiplier: " + e2.getLocalizedMessage(), e2);
						SwingTools.showSimpleErrorMessage("Could not create multiplier",
								"Cannot create multiplier: " + e2.getLocalizedMessage());
					} finally {
						for (Port port : toUnlock) {
							port.unlock();
						}
					}
				}

			};

			JMenuItem menuItem = new JMenuItem(addIOMultiplierActions);
			menuItem.setToolTipText(I18N.getGUILabel("cannot_connect.click_to_branch"));

			menu.add(menuItem);

			// show popup
			menu.show(parent, (int) popupLocation.getX(), (int) popupLocation.getY());

			ActionListener hideMenuTimer = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (menu.isVisible()) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								menu.setVisible(false);
							}
						});
					}

				}
			};

			final Timer timer = new Timer(HIDE_DELAY, hideMenuTimer);

			menuItem.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseEntered(MouseEvent e) {
					super.mouseEntered(e);
					timer.stop();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					super.mouseExited(e);
					timer.start();
				}
			});

			timer.start();
		}

	}
}
