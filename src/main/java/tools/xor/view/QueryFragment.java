/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

package tools.xor.view;

import tools.xor.EntityType;
import tools.xor.util.Vertex;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class QueryFragment implements Vertex
{
    public static final String ROOT_NAME = "<ROOT>";

    EntityType entityType;
    String ancestorPath;
    String alias;
    List<String> paths;
    int simpleCollectionCount;

    public EntityType getEntityType ()
    {
        return entityType;
    }

    public String getAlias ()
    {
        return alias;
    }

    public QueryFragment(EntityType type, String alias, String ancestorPath) {
        this.entityType = type;
        this.alias = alias;
        this.ancestorPath = ancestorPath;
        this.paths = new LinkedList<>();
        this.simpleCollectionCount = 0;
    }

    public void incrementSimpleCollectionCount() {
        this.simpleCollectionCount++;
    }

    @Override public String getName ()
    {
        return this.ancestorPath == null ? ROOT_NAME : this.ancestorPath;
    }

    public void addPath(String path) {
        this.paths.add(path);
    }

    @Override public String toString() {
        return getAlias();
    }

    public List<String> getPaths() {
        return Collections.unmodifiableList(this.paths);
    }
}
