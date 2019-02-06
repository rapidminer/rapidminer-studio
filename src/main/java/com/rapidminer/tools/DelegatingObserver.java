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
package com.rapidminer.tools;

/**
 * An observer that fires another observable on any update.
 * 
 * @author Simon Fischer
 * */
public class DelegatingObserver<T1, T2> implements Observer<T1> {

	private final AbstractObservable<T2> target;
	private final T2 argument;

	public DelegatingObserver(AbstractObservable<T2> target, T2 argument) {
		super();
		this.target = target;
		this.argument = argument;
	}

	@Override
	public void update(Observable<T1> observable, T1 arg) {
		target.fireUpdate(argument);
	}

}
