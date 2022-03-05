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

import java.util.Stack;

public class IdentityStack<T> extends Stack<T>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7633241118173209011L;

	/**
	 * Returns the index of the first occurrence of the specified element in
	 * this vector, searching forwards from {@code index}, or returns -1 if
	 * the element is not found.
	 * More formally, returns the lowest index {@code i} such that
	 * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o == get(i)))</tt>,
	 * or -1 if there is no such index.
	 *
	 * @param o element to search for
	 * @param index index to start searching from
	 * @return the index of the first occurrence of the element in
	 *         this vector at position {@code index} or later in the vector;
	 *         {@code -1} if the element is not found.
	 * @throws IndexOutOfBoundsException if the specified index is negative
	 */	
	@Override
	public synchronized int indexOf(Object o, int index) {
		for (int i = index ; i < elementCount ; i++)
			if (elementData[i]==o)
				return i;
		return -1;
	}	
}