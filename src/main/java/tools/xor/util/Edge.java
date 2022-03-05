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

package tools.xor.util;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;

public class Edge<V extends Vertex>
{

    public static final String NONE_OR_ONE = "0..1";
    public static final String EXACTLY_ONE = "1..1";
    public static final String ZERO_OR_MORE = "0..*";
    public static final String COL_FANOUT = "Σ";

    private final String name;
    private final V start;
    private final V end;
    private final boolean qualified;
    private final boolean reversed; // Denotes that the edge is the reverse of the actual relationship.
    // This is utilized for Topological sorting.

    public String getName ()
    {
        return name;
    }

    public String getDisplayName ()
    {
        return getName();
    }

    public boolean isQualified ()
    {
        return qualified;
    }

    public V getStart ()
    {
        return start;
    }

    public V getEnd ()
    {
        return end;
    }

    public Edge (String name, V start, V end)
    {
        this(name, start, end, false);
    }

    public Edge (String name, V start, V end, boolean qualify)
    {
        this(name, start, end, qualify, false);
    }

    public Edge (String name, V start, V end, boolean qualify, boolean reversed)
    {
        this.name = name;
        this.start = start;
        this.end = end;
        this.qualified = qualify;
        this.reversed = reversed;
    }

    @Override
    /**
     * Have to use a different prime number when incorporating boolean fields in hashCode
     */
    public int hashCode ()
    {
        int result = 17;
        result = 37 * result + name.hashCode();
        result = 37 * result + start.hashCode();
        result = 37 * result + end.hashCode();
        result = 41 * result + (qualified ? 1 : 0);
        result = 43 * result + (reversed ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) {
            return false;
        }

        Edge otherEdge = (Edge)other;

        return name.equals(otherEdge.name) &&
            start.equals(otherEdge.start) &&
            end.equals(otherEdge.end) &&
            qualified == otherEdge.qualified &&
            reversed == otherEdge.reversed;
    }

    public String getQualifiedName ()
    {
        if (qualified) {
            return name + Settings.PATH_DELIMITER;
        }
        else {
            return name;
        }
    }

    public Edge reverse ()
    {
        return new Edge(name, end, start, this.qualified, this.reversed ^ true);
    }

    public String getEndCardinality() {
        V edgeStart = start;
        if(reversed) {
            edgeStart = end;
        }

        if(edgeStart instanceof State) {
            Type type = ((State)edgeStart).getType();
            if(type instanceof EntityType && name != null && !"".equals(name)) {
                Property property = type.getProperty(name);
                if(property.isMany()) {
                    return ZERO_OR_MORE;
                } else if(property.isNullable()) {
                    return NONE_OR_ONE;
                } else {
                    return EXACTLY_ONE;
                }
            }
        }

        return null;
    }

    public boolean isUnlabelled() {
        return DFAtoNFA.UNLABELLED.equals(getName());
    }

    @Override
    public String toString ()
    {
        return start.getName() + "--" + getName() + "-->" + end.getName();
    }
}
