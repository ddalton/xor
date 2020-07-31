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

import java.util.ArrayList;
import java.util.List;

import tools.xor.Settings;

public abstract class QuerySupport
{
    protected List<String> augmenter;  // Additional columns for object reconstitution
                                       // Needs to be in dotten notation. Will be added to the attributelist in View


    public List<String> getAugmenter () {
        return augmenter;
    }

    public void setAugmenter (List<String> augmenter) {
        this.augmenter = augmenter;
    }


    public List<String> getColumns(View view) {
        List<String> result = new ArrayList<>(view.getAttributeList());

        if(augmenter != null) {
            result.addAll(augmenter);
        }

        return result;
    }

    public void deriveColumns(QueryTree queryTree, QueryHandle handle, Settings settings, AggregateTree aggregateTree, View view) {
        if(queryTree != null) {
            queryTree.generateFields(settings, aggregateTree);
            handle.setColumns(queryTree.getSelectedColumns());
        } else {
            handle.setColumns(getColumns(view));
        }
    }

    public void copy(QuerySupport object) {
        object.augmenter = augmenter != null ? new ArrayList<>(augmenter) : null;
    }
}
