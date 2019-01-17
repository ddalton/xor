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

import tools.xor.CallInfo;
import tools.xor.util.InterQuery;

import java.util.LinkedList;
import java.util.List;

/**
 * Goes through each QueryPiece and executes it.
 * The walk is done in a BFS manner.
 */
public class SerialDispatcher implements QueryDispatcher
{
    @Override public void execute (QueryTree<QueryPiece, InterQuery<QueryPiece>> qt, ObjectResolver resolver, CallInfo callInfo)
    {
        QueryPiece qp = qt.getRoot();
        List<QueryPiece> queries = new LinkedList<>();
        queries.add(qp);

        while(!queries.isEmpty()) {
            qp = queries.remove(0);

            Query query = qp.prepare(callInfo, resolver);
            if(query != null) {
                List records = query.getResultList(null, callInfo.getSettings());
                resolver.processRecords(records, qp, callInfo);
            }

            queries.addAll(qt.getChildren(qp));
        }

        resolver.postProcess();
    }
}
