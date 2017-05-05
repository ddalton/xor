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

package tools.xor;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import tools.xor.generator.Generator;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

import java.util.List;

public class StringType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public static final int DEFAULT_LENGTH = 255;
	public static final int MIN_LENGTH = 7; // to avoid empty strings in natural keys and reduce the occurrence of unique constraint violations

	public StringType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {

		Generator gen = ((ExtendedProperty)property).getGenerator(visitor.getRelationshipName());
		if(gen != null) {
			return gen.getStringValue(property, visitor);
		} else {
			int length = DEFAULT_LENGTH;
			if(property.getConstraints().containsKey(Constants.XOR.CONS_LENGTH)) {
				length = (int)property.getConstraints().get(Constants.XOR.CONS_LENGTH);
			}
			int stringLen = (int)(Math.random() * length);
			if (stringLen < MIN_LENGTH) {
				stringLen = (MIN_LENGTH > length) ? length : MIN_LENGTH;
			}
			return RandomStringUtils.randomAscii(stringLen);
		}
	}	
}
