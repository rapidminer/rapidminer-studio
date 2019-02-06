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
package com.rapidminer.operator;

import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Tools;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;


/**
 * An adapter class for the interface {@link ResultObject}. Implements most methods and can be used
 * if the subclass does not need to extend other classes. The method {@link #toResultString()}
 * delivers the return value of {@link #toString()}. The visualization components for the graphical
 * user interface is simply the HTML representation of the result string.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class ResultObjectAdapter extends AbstractIOObject implements ResultObject, LoggingHandler {

	private static final long serialVersionUID = -8621885253590411373L;
	private Annotations annotations = new Annotations();

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (annotations == null) {
			annotations = new Annotations();
		}
	}

	protected void cloneAnnotationsFrom(IOObject other) {
		this.annotations = other.getAnnotations().clone();
	}

	/** The default implementation returns the classname without package. */
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Adds an action to the list of Java Swing Actions which will shown in the visualization
	 * component. If the class implements Saveable an action for saving is already added.
	 * 
	 * @deprecated Action concept for GUI components removed from result objects
	 */
	@Deprecated
	protected void addAction(Action a) {}

	/**
	 * Returns a list of all actions which can be performed for this result object.
	 * 
	 * @deprecated Action concept for GUI components removed from result objects
	 */
	@Override
	@Deprecated
	public List<Action> getActions() {
		return new LinkedList<>();
	}

	/**
	 * The default implementation simply returns the result of the method {@link #toString()}.
	 */
	@Override
	public String toResultString() {
		return toString();
	}

	/**
	 * Returns the icon registered for this class on the {@link RendererService}. This method can be
	 * replaced by {@link RendererService#getIcon(Class)} if no instanciated object of this class is
	 * available.
	 * */
	@Override
	public Icon getResultIcon() {
		return RendererService.getIcon(this.getClass());
	}

	/**
	 * Encodes the given String as HTML. Only linebreaks and less then and greater than will be
	 * encoded.
	 */
	public static String toHTML(String string) {
		String str = string;
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(Tools.getLineSeparator(), "<br>");
		return str;
	}

	@Override
	public void log(String message, int level) {
		getLog().log(message, level);
	}

	@Override
	public void log(String message) {
		getLog().log(getName() + ": " + message);
	}

	@Override
	public void logNote(String message) {
		getLog().logNote(getName() + ": " + message);
	}

	@Override
	public void logWarning(String message) {
		getLog().logWarning(getName() + ": " + message);
	}

	@Override
	public void logError(String message) {
		getLog().logError(getName() + ": " + message);
	}

	@Override
	public Annotations getAnnotations() {
		return this.annotations;
	}
}
