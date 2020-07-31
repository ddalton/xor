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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.DataModel;

/**
 * Using JSON is a better alternative if possible as you don't have to maintain a DTO layer
 * 
 * @see ImmutableJsonTypeMapper
 * @author family
 *
 */
public class DTOTypeMapper extends AbstractTypeMapper {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final String DTO_SUFFIX = "VO";

	private String domainPackagePath;
	private String externalPackagePath;
	
    public DTOTypeMapper() {
        super();
    }
    
    public DTOTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) 
    {
        super(das, side, shapeName, persistenceManaged);
    }	

	public String getDomainPackagePath() {
		return domainPackagePath;
	}

	public void setDomainPackagePath(String domainPackagePath) {
		this.domainPackagePath = domainPackagePath;
	}

	public String getExternalPackagePath() {
		return externalPackagePath;
	}

	public void setExternalPackagePath(String externalPackagePath) {
		this.externalPackagePath = externalPackagePath;
	}
	
	@Override
    public String toDomain(String typeName) {
        Class<?> domainClass = getDomainClass(getJavaClass(typeName));
        return domainClass == null ? null : domainClass.getName();
    }

    private Class<?> getDomainClass(Class<?> externalClass) {
        try {
            if (externalClass.isArray())
                if (isExternal(externalClass.getComponentType().getName()))
                    throw new RuntimeException("Array of entity is not supported");
                else
                    return externalClass;

            if (isDomain(externalClass.getName()) || externalClass.isPrimitive())
                return externalClass;

            String fullClassName = externalClass.getCanonicalName();
            String toClassName = fullClassName;

            if (fullClassName.startsWith(externalPackagePath))
                // Change the package to the Domain package
                toClassName = fullClassName.replaceFirst(externalPackagePath, domainPackagePath);

            if (toClassName.endsWith(DTO_SUFFIX) && toClassName.startsWith(domainPackagePath)) {
                // Remove the VO_SUFFIX
                int toNameLen = toClassName.length();
                StringBuilder toNameBuf = (new StringBuilder(toClassName)).delete(toNameLen - DTO_SUFFIX.length(),
                        toNameLen);
                toClassName = toNameBuf.toString();
            }

            return Class.forName(toClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    @Override
    public String toExternal(String typeName) {
        Class<?> externalClass = getExternalClass(getJavaClass(typeName));
        return externalClass == null ? null : externalClass.getName();
    }

	@Override
	public Class<?> toExternal(Type type) {
	    Class<?> domainClass = type.getInstanceClass();
		if(domainClass.isArray())
			if(isExternal(domainClass.getComponentType().getName()))
					throw new RuntimeException("Array of entity is not supported");
			else
				return domainClass;		
		
		if(isExternal(domainClass.getName()) || domainClass.isPrimitive())
			return domainClass;
		
		String fullClassName = domainClass.getCanonicalName();
		String toClassName = fullClassName;

		if(fullClassName.startsWith(domainPackagePath))
			// Change the package to the external package
			toClassName = fullClassName.replaceFirst(domainPackagePath, externalPackagePath);  

		if(!toClassName.endsWith(DTO_SUFFIX) && toClassName.startsWith(externalPackagePath))
			// Adjust the class name to the VO class name
			toClassName = toClassName.concat(DTO_SUFFIX);                              

		try {
			return Class.forName(toClassName);
		} catch (ClassNotFoundException e) {
			logger.warn("Unable to find External class: " + toClassName);
		}	

		return null;		
	}
	
	private Class<?> getExternalClass(Class<?> domainClass) {
        try {
            if(domainClass.isArray())
                if(isExternal(domainClass.getComponentType().getName()))
                        throw new RuntimeException("Array of entity is not supported");
                else
                    return domainClass;     
            
            if(isExternal(domainClass.getName()) || domainClass.isPrimitive())
                return domainClass;
            
            String fullClassName = domainClass.getCanonicalName();
            String toClassName = fullClassName;

            if(fullClassName.startsWith(domainPackagePath))
                // Change the package to the external package
                toClassName = fullClassName.replaceFirst(domainPackagePath, externalPackagePath);  

            if(!toClassName.endsWith(DTO_SUFFIX) && toClassName.startsWith(externalPackagePath))
                // Adjust the class name to the VO class name
                toClassName = toClassName.concat(DTO_SUFFIX);                              

            return Class.forName(toClassName);            
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;            
	}
	
    @Override
    public boolean isExternal(String typeName) {
        return (typeName.startsWith(externalPackagePath) && typeName.endsWith(DTO_SUFFIX));
    }	
	
    @Override
    public boolean isDomain(String typeName) {
        return (typeName.startsWith(domainPackagePath) && !typeName.endsWith(DTO_SUFFIX));
    }	

	@Override
	protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
		return new DTOTypeMapper(das, side, shapeName, persistenceManaged);
	}
	
    @Override 
    public TypeMapper newInstance(MapperSide side) {
        return newInstance(side, null);
    }
    
    @Override 
    public TypeMapper newInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        DTOTypeMapper mapper = (DTOTypeMapper)createInstance(das, side, shapeName, persistenceManaged);
        mapper.setDomainPackagePath(getDomainPackagePath());
        mapper.setExternalPackagePath(getExternalPackagePath());

        return mapper;
    }      
}
