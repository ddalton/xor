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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import tools.xor.util.graph.StateGraph;

/**
 * @author Dilip Dalton
 * 
 */
public class ListType extends SimpleType {

	private Type ofType;
	private boolean allowsNullValues = true;

	public ListType(Class<?> clazz) {
		super(clazz);
	}

	public ListType(Class<?> clazz, Type ofType) {
		super(clazz);

		this.ofType = ofType;
	}

	@Override
	public Object newInstance(Object instance) {
		return new ArrayList<Object>();
	}

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		return super.generateArray(settings, property, rootedAt, entitiesToChooseFrom, visitor);
	}	
	
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_ARRAY_TYPE;
    }

	@Override
	public Type ofType ()
	{
		return this.ofType;
	}

	public boolean allowsNullValues ()
	{
		return allowsNullValues;
	}

	public void setAllowsNullValues (boolean allowsNullValues)
	{
		this.allowsNullValues = allowsNullValues;
	}

	public TypeKind getKind() {
		return TypeKind.LIST;
	}

	@Override
	public String getGraphQLName ()
	{
		return String.format("[%s%s]", ofType.getGraphQLName(), allowsNullValues() ? "" : "!");
	}
}
