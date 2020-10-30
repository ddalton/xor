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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.generator.Generator;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

public class ShortType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private short min = Short.MIN_VALUE;
	private short max = Short.MAX_VALUE;	

	public short getMin() {
		return min;
	}

	public void setMin(short min) {
		this.min = min;
	}

	public short getMax() {
		return max;
	}

	public void setMax(short max) {
		this.max = max;
	}

	public ShortType(Class<?> clazz) {
		super(clazz);
	}	

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {

		Generator gen = ((ExtendedProperty)property).getGenerator(visitor.getRelationshipName());
		if(gen != null) {
			return gen.getShortValue(visitor);
		}

		// The range can overflow, so we use int type
		int range = (getMax() - getMin());
		return (short) (getMin() + (range == 0 ? 0 : ClassUtil.nextDouble()*range));
	}	
	
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_INTEGER_TYPE;
    }	
}
