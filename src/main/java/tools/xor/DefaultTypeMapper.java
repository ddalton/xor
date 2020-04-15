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

import tools.xor.service.DataAccessService;
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
    
    public DefaultTypeMapper(DataAccessService das, MapperSide side, String shapeName) 
    {
        super(das, side, shapeName);
    }
    
	/* (non-Javadoc)
	 * @see TypeMapper#toReference(java.lang.Class)
	 */
	@Override
	public Class<?> toDomain(Class<?> externalClass) {
		return externalClass;
	}

	/* (non-Javadoc)
	 * @see TypeMapper#toExternal(java.lang.Class)
	 */
	@Override
	public Class<?> toExternal(Class<?> referenceClass) {
		return referenceClass;		
	}	
	
	@Override
	public Class<?> getMappedClass(Class<?> clazz, CallInfo callInfo) {
		return clazz;
	}

	@Override
	protected TypeMapper createInstance(DataAccessService das, MapperSide side, String shapeName) {
		return new DefaultTypeMapper(das, side, shapeName);
	}

	@Override 
	public TypeMapper newInstance(MapperSide side) {
		return newInstance(side, null);
	}
    
    @Override 
    public TypeMapper newInstance(DataAccessService das, MapperSide side, String shapeName) {
        return createInstance(das, side, shapeName);
    }    
    
    @Override
    public Shape getDomainShape() {
        if(this.domainShape == null) {
            this.domainShape = getDAS().getShape(getShapeName());
            
            if(this.domainShape == null) {
                // create this shape
                this.domainShape = getDAS().createShape(getShapeName());
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
