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

package tools.xor.view.join;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import tools.xor.BusinessObject;

public class SortMerge implements JoinStrategy {
	public List<BusinessObject> execute(List<BusinessObject> list1, List<BusinessObject> list2, List<Comparator> sortColumns) {
		List<BusinessObject> result = new ArrayList<BusinessObject>();
		
		int list1index = 0;
		int list2index = 0;
		BusinessObject list1Object = null;
		BusinessObject list2Object = null;
		while(list1index < list1.size() && list2index < list2.size()) {
			if(list1index == list1.size()) {
				for(int i = list2index; i < list2.size(); i++)
					result.add(list2.get(i));
				return result;
			}
			if(list2index == list2.size()) {
				for(int i = list1index; i < list1.size(); i++)
					result.add(list1.get(i));
				return result;
			}
			list1Object = list1.get(list1index);
			list2Object = list2.get(list2index);
			
			for(int i = 0; i < sortColumns.size(); i++) {
				Comparator c = sortColumns.get(i);
				int cresult = c.compare(list1Object, list2Object);
				if(cresult == 0) { // equal
					if(i == sortColumns.size()-1) {
						result.add(list1Object);
						list1index++;
					} else
					continue;
				} else if(cresult == -1) {
					result.add(list1Object);
					list1index++;
					break;
				} else {
					result.add(list2Object);
					list2index++;
					break;
				}
			}
		}

		return result;
	}
}
