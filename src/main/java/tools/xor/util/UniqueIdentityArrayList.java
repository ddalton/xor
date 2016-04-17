/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.util;

import java.util.Collection;
import java.util.LinkedList;


public class UniqueIdentityArrayList<T> extends IdentityArrayList<T>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7633241118173209011L;

	@Override
	public boolean add(T e) {
		if (this.contains(e)) {
			return false;
		}
		else {
			return super.add(e);
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> collection) {
		Collection<T> copy = new LinkedList<T>(collection);
		copy.removeAll(this);
		return super.addAll(copy);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> collection) {
		Collection<T> copy = new LinkedList<T>(collection);
		copy.removeAll(this);
		return super.addAll(index, copy);
	}

	@Override
	public void add(int index, T element) {
		if (this.contains(element)) {
			return;
		}
		else {
			super.add(index, element);
		}
	}
}