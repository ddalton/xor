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

import java.io.Serializable;

/**
 * Uniquely identifies a persistent entity.
 */
public abstract class AbstractEntityKey implements QueryKey, Serializable
{
    private final String entityTypeName; // For an external entity any name that along with the identifier uniquely identifies the entity
    private final String path; // If present, then two entities are considered distinct if they are on a different path though they may have the same id

    public AbstractEntityKey(String entityTypeName, String path) {

        if ( entityTypeName == null || entityTypeName.trim().equals("")) {
            throw new IllegalStateException( "Entity type name needs to be provided" );
        }

        this.entityTypeName = entityTypeName;
        this.path = path;
    }

    protected int generateHashCode() {
        int result = 17;
        result = 37 * result + entityTypeName.hashCode();
        result = (this.path != null) ? (37 * result + this.path.hashCode()) : result;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if(other == null)
            return false;

        if (!(other instanceof AbstractEntityKey))
            return false;

        AbstractEntityKey otherKey = (AbstractEntityKey) other;

        // Both should have path initialized or not initialized
        if( this.path == null ^ otherKey.path == null) {
            return false;
        }

        // If both paths are initialized then they should be equal
        if (this.path != null && !this.path.equals(otherKey.path)) {
            return false;
        }

        // check they are of the same type
        return otherKey.entityTypeName.equals(this.entityTypeName);
    }

    protected String getEntityTypeName() {
        return this.entityTypeName;
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
