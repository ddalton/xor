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

import tools.xor.service.DataModel;
import tools.xor.service.Shape;

/**
 * The DefaultTypeMapper does not need to manage dual type systems.
 * The dynamic and domain represent the same type system.
 * 
 * @author Dilip Dalton
 *
 */
public class DefaultTypeMapper extends AbstractTypeMapper {
   
    public DefaultTypeMapper() {
        super();
    }
    
    public DefaultTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) 
    {
        super(das, side, shapeName, persistenceManaged);
    }
    
    @Override
    public String toDomain(String typeName) {
        return typeName;
    }    
	
	public String toExternal(String typeName) {
	    return typeName;
	}

	@Override
	public Class<?> toExternal(Type type) {
		return type.getInstanceClass();		
	}	
	
    public String getMappedType(String typeName, CallInfo callInfo) {
        return typeName;
    }

	@Override
	protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
		return new DefaultTypeMapper(das, side, shapeName, persistenceManaged);
	}

	@Override 
	public TypeMapper newInstance(MapperSide side) {
		return newInstance(side, null);
	}
    
    @Override 
    public TypeMapper newInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        return createInstance(das, side, shapeName, persistenceManaged);
    }    
    
    @Override
    public Shape getDomainShape() {
        if(this.domainShape == null) {
            this.domainShape = getModel().getShape(getShapeName());
            
            if(this.domainShape == null) {
                // create this shape
                this.domainShape = getModel().createShape(getShapeName());
            }
        }

        return this.domainShape;
    }

    @Override
    public Shape getDynamicShape() {
        if(this.dynamicShape == null) {
            // this call initializes the dynamic shape
            this.getDomainShape();
            
            // By default the dynamic shape is the same as the domain shape
            // implementations can override this behavior
            this.dynamicShape = this.domainShape;            
        }
        return this.dynamicShape;
    }        
}
