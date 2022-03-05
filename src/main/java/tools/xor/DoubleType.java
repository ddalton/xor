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

public class DoubleType extends SimpleType implements Scalar {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private double min = Double.MIN_VALUE;
	private double max = Double.MAX_VALUE;	

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public DoubleType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public String getGraphQLName () {
		// GraphQL Float type represents double-precision floating point numbers
		return Scalar.FLOAT;
	}

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {

		Generator gen = ((ExtendedProperty)property).getGenerator(visitor.getRelationshipName());
		if(gen != null) {
			return gen.getDoubleValue(visitor);
		}

		double range = getMax() - getMin();
		return getMin() + (range == 0 ? 0 : ClassUtil.nextDouble()*range);
	}	
	
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_NUMBER_TYPE;
    }	
}
