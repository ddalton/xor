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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Uniquely identifies a persistent entity based on one or more entity attributes.
 */
public final class NaturalEntityKey extends AbstractEntityKey
{
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> key;
    private final int hashCode;

    public NaturalEntityKey (Map<String, Object> naturalKey, String entityTypeName)
    {
        this(naturalKey, entityTypeName, null);
    }

    public NaturalEntityKey (Map<String, Object> naturalKey, String entityTypeName, String path)
    {
        super(entityTypeName, path);
        if (naturalKey == null || naturalKey.size() == 0) {
            throw new IllegalStateException("null identifier");
        }

        Map<String, Object> aMap = new HashMap<>(naturalKey);
        this.key = Collections.unmodifiableMap(aMap);
        this.hashCode = generateHashCode();
    }

    @Override
    protected int generateHashCode ()
    {
        return 37 * super.generateHashCode() + key.hashCode();
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof NaturalEntityKey))
            return false;

        if (key.size() == 0) {
            return false;
        }

        // don't consider equality of an empty key
        boolean hasValue = false;
        for (Object keypart : key.values()) {
            if (keypart == null || "".equals(keypart.toString())) {
                continue;
            }
            hasValue = true;
            break;
        }
        if (!hasValue) {
            return false;
        }

        NaturalEntityKey otherKey = (NaturalEntityKey)other;

        // check they are of the same type
        return super.equals(otherKey) && otherKey.key.equals(this.key);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
