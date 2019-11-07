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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.Tools;


/**
 * Input for Operator.apply(). Instances of this class are containers for IOObjects. They are
 * available by calling one of the <tt>getInput</tt> methods. The operator can choose between
 * keeping the IOObject in the container or delete it using it.<br>
 * 
 * From Version 5.0 on, IOContainers are only used for compatibility reasons and are only a
 * collection of IOObjects. IOContainers are no longer passed between Operators and hence most of
 * its functionality is obsolete.
 * 
 * @see com.rapidminer.operator.IOObject
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class IOContainer implements Serializable {

	private static final long serialVersionUID = 8152465082153754473L;

	private final List<IOObject> ioObjects;

	public static final IOContainer DUMMY_IO_CONTAINER = new IOContainer(new IOObject[] { new ResultObjectAdapter() {

		private static final long serialVersionUID = -5877096753744650074L;

		@Override
		public String getName() {
			return "Dummy";
		}

		@Override
		public String toString() {
			return "No intermediate results for this operator";
		}

	} });

	/** Creates a new and empty IOContainer. */
	public IOContainer() {
		this(new IOObject[0]);
	}

	/**
	 * Creates a new IOContainer containing the contents of the Collection which must contain only
	 * IOObjects.
	 */
	public IOContainer(Collection<? extends IOObject> objectCollection) {
		ioObjects = new ArrayList<>(objectCollection.size());
		ioObjects.addAll(objectCollection);
	}

	public IOContainer(IOObject... objectArray) {
		ioObjects = new ArrayList<>(objectArray.length);
		for (int i = 0; i < objectArray.length; i++) {
			ioObjects.add(objectArray[i]);
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("IOContainer (" + ioObjects.size() + " objects):" + Tools.getLineSeparator());
		for (IOObject current : ioObjects) {
			if (current != null) {
				result.append(current.toString() + Tools.getLineSeparator() + (current.getSource() != null
						? "(created by " + current.getSource() + ")" + Tools.getLineSeparator() : ""));
			}
		}
		return result.toString();
	}

	/** Returns the number of {@link IOObject}s in this container. */
	public int size() {
		return ioObjects.size();
	}

	/** Returns the n-th {@link IOObject} in this container. */
	public IOObject getElementAt(int index) {
		return ioObjects.get(index);
	}

	/** Removes and returns the n-th {@link IOObject} in this container. */
	public IOObject removeElementAt(int index) {
		return ioObjects.remove(index);
	}

	/**
	 * Returns all IOObjects.
	 */
	public IOObject[] getIOObjects() {
		return ioObjects.toArray(new IOObject[ioObjects.size()]);
	}

	/** Gets the first IOObject which is of class cls. */
	public <T extends IOObject> T get(Class<T> cls) throws MissingIOObjectException {
		return getInput(cls, 0, false);
	}

	/** Gets the nr-th IOObject which is of class cls. */
	public <T extends IOObject> T get(Class<T> cls, int nr) throws MissingIOObjectException {
		return getInput(cls, nr, false);
	}

	/**
	 * Removes the first IOObject which is of class cls. The removed object is returned.
	 */
	public <T extends IOObject> T remove(Class<T> cls) throws MissingIOObjectException {
		return getInput(cls, 0, true);
	}

	/**
	 * Removes the nr-th IOObject which is of class cls. The removed object is returned.
	 */
	public <T extends IOObject> T remove(Class<T> cls, int nr) throws MissingIOObjectException {
		return getInput(cls, nr, true);
	}

	/**
	 * Returns true if this IOContainer containts an IOObject of the desired class.
	 */
	public boolean contains(Class<? extends IOObject> cls) {
		for (IOObject object : ioObjects) {
			if (((cls.isInstance(object)) || isToExampleSetConvertible(cls, object))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the nr-th IOObject which is of class cls. If remove is set to true, the object is
	 * afterwards removed from this IOContainer.
	 */
	private <T extends IOObject> T getInput(Class<T> cls, int nr, boolean remove) throws MissingIOObjectException {
		int n = 0;
		Iterator<IOObject> i = ioObjects.iterator();
		while (i.hasNext()) {
			IOObject object = i.next();
			if (((cls.isInstance(object)) || isToExampleSetConvertible(cls, object))) {
				if (n == nr) {
					if (remove) {
						i.remove();
					}
					if (cls.isInstance(object)) {
						return cls.cast(object);
					} else {
						return cls.cast(BeltConverter.convertSequentially((IOTable) object));
					}
				} else {
					n++;
				}
			}
		}
		throw new MissingIOObjectException(cls);
	}

	/**
	 * Check if the desired class is ExampleSet and the object an {@link IOTable} so that conversion is possible.
	 */
	private <T extends IOObject> boolean isToExampleSetConvertible(Class<T> cls, IOObject object) {
		return object instanceof IOTable && cls.equals(ExampleSet.class)
				//cannot convert custom columns
				&& ((IOTable)object).getTable().select().ofTypeId(Column.TypeId.CUSTOM).labels().isEmpty();
	}

	/**
	 * Creates a new IOContainer by adding all IOObjects of this container to the given IOObject.
	 */
	public IOContainer append(IOObject object) {
		return append(new IOObject[] { object });
	}

	/**
	 * Creates a new IOContainer by adding all IOObjects of this container to the given IOObjects.
	 */
	public IOContainer append(IOObject[] output) {
		List<IOObject> newObjects = new LinkedList<>();
		for (int i = 0; i < output.length; i++) {
			newObjects.add(output[i]);
		}
		newObjects.addAll(ioObjects);
		return new IOContainer(newObjects);
	}

	/**
	 * Creates a new IOContainer by adding the given object before the IOObjects of this container.
	 */
	public IOContainer prepend(IOObject object) {
		return prepend(new IOObject[] { object });
	}

	/**
	 * Creates a new IOContainer by adding the given objects before the IOObjects of this container.
	 */
	public IOContainer prepend(IOObject[] output) {
		List<IOObject> newObjects = new LinkedList<>();
		newObjects.addAll(ioObjects);
		for (int i = 0; i < output.length; i++) {
			newObjects.add(output[i]);
		}
		return new IOContainer(newObjects);
	}

	/** Appends this container's IOObjects to output. */
	public IOContainer append(Collection<IOObject> output) {
		List<IOObject> newObjects = new LinkedList<>();
		newObjects.addAll(output);
		newObjects.addAll(ioObjects);
		return new IOContainer(newObjects);
	}

	/** Copies the contents of this IOContainer by invoking the method copy of all IOObjects. */
	public IOContainer copy() {
		List<IOObject> clones = new LinkedList<>();
		Iterator<IOObject> i = ioObjects.iterator();
		while (i.hasNext()) {
			clones.add((i.next()).copy());
		}
		return new IOContainer(clones);
	}

	/** Removes all Objects from this IOContainer. */
	public void removeAll() {
		ioObjects.clear();
	}

	public List<IOObject> asList() {
		return Collections.unmodifiableList(ioObjects);
	}
}
