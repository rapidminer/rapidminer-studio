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
package com.rapidminer.gui.tools.actions;

import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * This action is meant for actions that depend on a selection but are present all the time.
 * Subclasses will get the currently selected object automatically and can perform their action
 * depending on it. Additionally if included somewhere these actions can be enabled or disabled
 * depending on the current selection. GUI classes using this kind of action need to call
 * {@link #selectionChanged()} whenever the selection changes to update the state of the action.
 * 
 * @author Sebastian Land
 */
public abstract class SelectionDependentAction extends ResourceAction {

	/**
	 * Interface for all selection dependencies. These actions can for example be dependent on a
	 * Tree selection, or list selection.
	 * 
	 * @author Sebastian Land
	 */
	public static interface SelectionDependency {

		/**
		 * This method has to return the currently selected object or null if nothing is selected.
		 */
		public Object getSelectedObject();
	}

	private static final long serialVersionUID = 1L;

	private SelectionDependency dependency = null;

	public SelectionDependentAction(boolean smallIcon, String i18nKey, Object... i18nArgs) {
		super(smallIcon, i18nKey, i18nArgs);
	}

	@Override
	public final void loggedActionPerformed(ActionEvent e) {
		if (dependency != null) {
			actionPerformed(e, dependency.getSelectedObject());
		}
	}

	/**
	 * Subclasses need to implement this method in order to be able to perform the action on the
	 * given object. The selectedObject might be null, since some actions might be performable
	 * without a selection.
	 */
	protected abstract void actionPerformed(ActionEvent e, Object selectedObject);

	/**
	 * This might be called whenever the selection of the given {@link SelectionDependency} changes.
	 * The state of this action will then be adapted automatically.
	 */
	public void selectionChanged() {
		setEnabled(dependency != null && isEnabledForSelection(dependency.getSelectedObject()));
	}

	/**
	 * This sets the dependency of this action. This method has to be called before the first action
	 * can be performed.
	 */
	public void setDependency(SelectionDependency dependency) {
		this.dependency = dependency;
	}

	/**
	 * This method must be implemented by subclasses to indicate if this action is available for
	 * this kind of object. The selected object might be null since some actions might be possible
	 * even without selection.
	 */
	protected abstract boolean isEnabledForSelection(Object selectedObject);

	/**
	 * Creates a clone of this action.
	 */
	@Override
	public Object clone() {
		try {
			SelectionDependentAction clone = (SelectionDependentAction) super.clone();
			clone.setDependency(dependency);
			return clone;
		} catch (CloneNotSupportedException e) {
			// can't happen
			return null;
		}
	}
}
