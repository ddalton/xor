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

package tools.xor.view.expression;

import java.util.Comparator;
import java.util.List;

import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty;

public class AscHandler extends FunctionHandler implements Comparator {

	@Override
	public String getQueryString() {
		return getNormalizedAttributeName() + " ASC";
	}

	@Override
	public void init(List<String> args) {
		normalizedNames.put(args.get(0), null);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!DescHandler.class.isAssignableFrom(o.getClass()))
			return false;
		
		DescHandler other = (DescHandler) o;
		return this.getAttributeName().equals(other.getAttributeName());
	}	

	@Override
	public int compare(Object o1, Object o2) {
		// Only works with Data objects
		if(!BusinessObject.class.isAssignableFrom(o1.getClass()) || !BusinessObject.class.isAssignableFrom(o1.getClass()))
			throw new RuntimeException("Only works with data objects");
		
		BusinessObject do1 = (BusinessObject) o1;
		BusinessObject do2 = (BusinessObject) o2;
		
		Object do1Value = ((ExtendedProperty)do1.getInstanceProperty(getAttributeName())).getValue(do1);
		Object do2Value = ((ExtendedProperty)do2.getInstanceProperty(getAttributeName())).getValue(do2);
		
		if(do1Value == null && do2Value == null)
			return 0;
		
		if( (do1Value != null && !Comparable.class.isAssignableFrom(do1Value.getClass())) ||
				(do2Value != null && !Comparable.class.isAssignableFrom(do2Value.getClass())) 
			)
				throw new RuntimeException("The objects need to implement the Comparable interface");
		
		// They should implement the comparable interface
		Comparable c1 = (Comparable) do1Value;
		Comparable c2 = (Comparable) do2Value;
		
		if(c1 == null) 
			return -1;
		else if(c2 == null)
			return 1;
		
		return c1.compareTo(c2);
	}
}
