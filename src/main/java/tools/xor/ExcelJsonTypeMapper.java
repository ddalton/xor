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
import tools.xor.util.CreationStrategy;
import tools.xor.util.ExcelJsonCreationStrategy;
import tools.xor.util.ObjectCreator;

public class ExcelJsonTypeMapper extends MutableJsonTypeMapper {
    
    public ExcelJsonTypeMapper() {
        super();
    }   
    
    public ExcelJsonTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) 
    {
        super(das, side, shapeName, persistenceManaged);
    }    

	@Override
	protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
		return new ExcelJsonTypeMapper(das, side, shapeName, persistenceManaged);
	}
	
	@Override
	public CreationStrategy getCreationStrategy(ObjectCreator oc) {
        if(getSide() == MapperSide.EXTERNAL) {
            return new ExcelJsonCreationStrategy(oc);
        }
        
        return getDomainCreationStrategy(oc);   
	}		
}
