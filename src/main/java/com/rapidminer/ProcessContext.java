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
package com.rapidminer;

import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.libraries.OperatorLibrary;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.container.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * <p>
 * The process context holds some data controlling the execution of a {@link Process}. This includes
 * connections of the input and output ports of the root operator to repository locations as well as
 * the definition of macros.
 * </p>
 * <p>
 * The fact that this data is defined outside the process itself is particularly useful if this
 * process is offered as a service, so it can be adapted easily. Furthermore, this saves the process
 * designer from defining data reading and storing operators at the beginning and at the end of the
 * process.
 * </p>
 * <p>
 * Note: A ProcessContext is not necessarily associate with a {@link Process}. E.g., if a process is
 * run remotely, it does not necessarily exist on the machine that prepares the context.
 * </p>
 * <p>
 * Since this class acts merely as a data container, it has public getter and setter methods which
 * return references to the actual data (as opposed to immutable views). In order to trigger an
 * update, call a setter method rather than adding to the lists, which is invisible to the process
 * context.
 * </p>
 * <p>
 * The data is saved as strings rather than, e.g. using {@link RepositoryLocation}s.
 * </p>
 * <p>
 * Since this class is saved as a Lob with the ProcessExecutionParameters entity, serializability
 * must be ensured. This is guaranteed by the fact that this class only contains {@link List}s of
 * strings or {@link Pair}s of strings, where {@link Pair} is serializable.
 * </p>
 * 
 * @author Simon Fischer
 */
public class ProcessContext extends AbstractObservable<ProcessContext> implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<String> inputRepositoryLocations = new ArrayList<>();

	private List<String> outputRepositoryLocations = new ArrayList<>();

	private List<Pair<String, String>> macros = new LinkedList<>();

	private List<Pair<OperatorLibrary, String>> libraryLocations = new LinkedList<>();

	public ProcessContext() {}

	public List<String> getInputRepositoryLocations() {
		return Collections.unmodifiableList(inputRepositoryLocations);
	}

	public void setInputRepositoryLocations(List<String> inputRepositoryLocations) {
		if (inputRepositoryLocations.contains(null)) {
			throw new NullPointerException("Null elements not allowed");
		}
		this.inputRepositoryLocations = inputRepositoryLocations;
		fireUpdate(this);
	}

	public List<String> getOutputRepositoryLocations() {
		return Collections.unmodifiableList(outputRepositoryLocations);
	}

	public void setOutputRepositoryLocations(List<String> outputRepositoryLocations) {
		if (outputRepositoryLocations.contains(null)) {
			throw new NullPointerException("Null elements not allowed");
		}
		this.outputRepositoryLocations = outputRepositoryLocations;
		fireUpdate(this);
	}

	public List<Pair<String, String>> getMacros() {
		return macros;
	}

	/**
	 * Updates the given macro with the given value. Fires an update after applying the changes.
	 * 
	 * @param macroIndex
	 *            index of the macro in the {@link #getMacros()} list
	 * @param pairIndex
	 *            0 for first value; 1 for second value
	 * @param newValue
	 */
	public void updateMacroValue(int macroIndex, int pairIndex, String newValue) {
		switch (pairIndex) {
			case 0:
				getMacros().get(macroIndex).setFirst(newValue);
				fireUpdate(this);
				break;
			case 1:
				getMacros().get(macroIndex).setSecond(newValue);
				fireUpdate(this);
				break;
			default:
				throw new IndexOutOfBoundsException(pairIndex + " > 1");
		}
	}

	/** Adds a macro to the list or sets an existing one. */
	public void addMacro(Pair<String, String> macro) {
		for (Pair<String, String> existingMacro : this.macros) {
			if (existingMacro.getFirst().equals(macro.getFirst())) {
				// overwrite existing
				existingMacro.setSecond(macro.getSecond());
				return;
			}
		}
		this.macros.add(macro);
		fireUpdate(this);
	}

	/** Removes a macro from the list */
	public void removeMacro(int index) {
		this.macros.remove(index);
		fireUpdate(this);
	}

	public void setMacros(List<Pair<String, String>> macros) {
		this.macros = macros;
		fireUpdate(this);
	}

	public void setOutputRepositoryLocation(int index, String location) {
		if (location == null) {
			throw new NullPointerException("Null location not allowed");
		}
		while (outputRepositoryLocations.size() <= index) {
			outputRepositoryLocations.add("");
		}
		outputRepositoryLocations.set(index, location);
		fireUpdate();
	}

	public void setInputRepositoryLocation(int index, String location) {
		if (location == null) {
			throw new NullPointerException("Null location not allowed");
		}
		while (inputRepositoryLocations.size() <= index) {
			inputRepositoryLocations.add("");
		}
		inputRepositoryLocations.set(index, location);
		fireUpdate();
	}

	public void removeOutputLocation(int rowIndex) {
		outputRepositoryLocations.remove(rowIndex);
	}

	public void removeInputLocation(int rowIndex) {
		inputRepositoryLocations.remove(rowIndex);
	}

	public void addOutputLocation(String location) {
		if (location == null) {
			throw new NullPointerException("Location must not be null");
		}
		outputRepositoryLocations.add(location);
	}

	public void addInputLocation(String location) {
		if (location == null) {
			throw new NullPointerException("Location must not be null");
		}
		inputRepositoryLocations.add(location);
	}

	/**
	 * Merges the current context with the given one. Macros will be simply added, input and output
	 * locations override their respective counterparts if not null. This modifies this instance.
	 */
	public void superimpose(ProcessContext other) {
		if (other == null) {
			return;
		}
		for (Pair<String, String> macro : other.macros) {
			this.macros.add(macro);
		}

		for (int i = 0; i < other.inputRepositoryLocations.size(); i++) {
			String loc = other.inputRepositoryLocations.get(i);
			if ((loc != null) && !loc.isEmpty()) {
				this.setInputRepositoryLocation(i, loc);
			}
		}

		for (int i = 0; i < other.outputRepositoryLocations.size(); i++) {
			String loc = other.outputRepositoryLocations.get(i);
			if ((loc != null) && !loc.isEmpty()) {
				this.setOutputRepositoryLocation(i, loc);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Macros: ").append(getMacros());
		b.append("; Input: ").append(getInputRepositoryLocations());
		b.append("; Output: ").append(getOutputRepositoryLocations());
		return b.toString();
	}

	/**
	 * This returns all loaded OperatorLibries that should be used within this process.
	 */
	public List<OperatorLibrary> getOperatorLibraries() {
		return Collections.emptyList();
	}

	public void addOperatorLibrary(OperatorLibrary library, String location) {
		libraryLocations.add(new Pair<>(library, location));
		try {
			library.registerOperators();
		} catch (OperatorCreationException e) {
			e.printStackTrace();
			// TODO: This is unnecessary if operators aren't initialized during registration!
		}
	}
}
