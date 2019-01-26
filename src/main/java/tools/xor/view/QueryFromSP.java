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

import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator;

public class QueryFromSP implements QueryBuilderStrategy
{
    private final View view;

    public QueryFromSP (View view)
    {
        this.view = view;
    }

    @Override public Query construct(Settings settings)
    {
        StoredProcedure querySP = view.getStoredProcedure(settings.getAction());

        Query query = null;
        if(querySP != null) {
            query = settings.getPersistenceOrchestrator().getQuery(
                querySP.getName(),
                PersistenceOrchestrator.QueryType.SP,
                querySP,
                settings);

            if (querySP.getResultList() == null || querySP.getResultList().size() == 0) {
                throw new IllegalArgumentException("Columns names need to be provided in resultList");
            }
            query.setColumns(view.getAttributeList());
        }

        return query;
    }
}
