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
package com.rapidminer.gui.operatormenu;

import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.OperatorService.OperatorServiceListener;
import com.rapidminer.tools.documentation.OperatorDocBundle;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;


/**
 * This is the abstract superclass for all menu containing operators. An operator menu can be used
 * for selecting new operators (see {@link NewOperatorMenu}) or replacing operators (see
 * {@link ReplaceOperatorMenu}).
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public abstract class OperatorMenu extends ResourceMenu implements OperatorServiceListener {

	private static final long serialVersionUID = 2685621612717488446L;

	public static final OperatorMenu NEW_OPERATOR_MENU = new NewOperatorMenu();

	public static final OperatorMenu REPLACE_OPERATOR_MENU = new ReplaceOperatorMenu(false);

	public static final OperatorMenu REPLACE_OPERATORCHAIN_MENU = new ReplaceOperatorMenu(true);

	private boolean onlySubprocesses;

	protected OperatorMenu(String key, boolean onlySubprocesses) {
		super(key);
		this.onlySubprocesses = onlySubprocesses;

		addMenu(OperatorService.getGroups(), this, onlySubprocesses);

		OperatorService.addOperatorServiceListener(this);
	}

	public void addMenu(GroupTree group, JMenu menu, boolean onlyChains) {
		Iterator<? extends GroupTree> gi = group.getSubGroups().iterator();
		while (gi.hasNext()) {
			GroupTree subGroup = gi.next();
			OperatorGroupMenu subMenu = new OperatorGroupMenu(subGroup.getName());
			addMenu(subGroup, subMenu, onlyChains);
			// do not add group named "deprecated" to menu
			if (subMenu.getItemCount() > 0 && !"deprecated".equals(subGroup.getKey())) {
				menu.add(subMenu);
			}
		}
		Iterator<OperatorDescription> i = group.getOperatorDescriptions().iterator();
		while (i.hasNext()) {
			final OperatorDescription description = i.next();
			if ((!onlyChains) || OperatorChain.class.isAssignableFrom(description.getOperatorClass())) {
				JMenuItem item = null;
				Icon icon = description.getSmallIcon();
				if (icon == null) {
					item = new JMenuItem(description.getName());
				} else {
					item = new JMenuItem(description.getName(), icon);
				}
				String descriptionText = description.getShortDescription();
				// if (descriptionText == null) {
				// descriptionText = description.getShortDescription();
				// }

				StringBuffer toolTipText = new StringBuffer("<b>Description: </b>" + descriptionText);
				Operator operator = null;
				try {
					operator = description.createOperatorInstance();
				} catch (OperatorCreationException e1) {
					// do nothing
				}
				item.setToolTipText("<html><div style=\"width:300px\">" + toolTipText.toString() + "</div></html>");
				item.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						performAction(description);
					}
				});

				if ((description.getDeprecationInfo() != null)
						|| ((operator != null) && (operator.getOperatorDescription().getDeprecationInfo() != null))) {
					item.setForeground(Color.LIGHT_GRAY);
				}

				menu.add(item);
			}
		}
	}

	public abstract void performAction(OperatorDescription description);

	@Override
	public void operatorRegistered(OperatorDescription description, OperatorDocBundle bundle) {
		removeAll();

		// then add everything back
		addMenu(OperatorService.getGroups(), this, onlySubprocesses);
	}

	@Override
	public void operatorUnregistered(OperatorDescription description) {
		removeAll();

		// then add everything back
		addMenu(OperatorService.getGroups(), this, onlySubprocesses);
	}
}
