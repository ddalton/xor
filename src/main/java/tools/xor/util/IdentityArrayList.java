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

import java.util.ArrayList;

public class IdentityArrayList<T> extends ArrayList<T>
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -7633241118173209011L;

	@Override
	public boolean contains(Object elem)
    {
        return indexOf(elem) >= 0;
    }

	@Override
    public int indexOf(Object elem)
    {
        for(int i = 0; i < size(); i++)
            if(elem == get(i))
                return i;
        return -1;
    }

	@Override
    public int lastIndexOf(Object elem)
    {
        for(int i = size() - 1; i >= 0; i--)
            if(elem == get(i))
                return i;
        return -1;
    }
	
	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		if(index == -1)
			return false;
		
		remove(index);
		
		return true;
	}
}