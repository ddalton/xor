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

package tools.xor;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

public class HierarchyGenerator extends DefaultGenerator implements GeneratorDriver, Iterator<HierarchyGenerator>
{
    private static final String PATH_DELIM = "/";

    private StateGraph.ObjectGenerationVisitor visitor;
    private HierarchyGenerator childGenerator;
    private HierarchyGenerator parentGenerator;
    private SharedCounterGenerator idGenerator;   // If populated helps to additionally track an id. There should be separate one for each HierarchyGenerator.
    private int maxInLevel;                         // max number of items in a particular level for a path
    private int total;
    private int counter;                          // current position of item that was last generated
    private int invocationCount;
    private String pathPart;
    private String delim;
    private HierarchyGenerator current = this;

    // Will be used to find the sizes > 2
    static int[] PRIMES = {
        2,
        3,
        5,
        7,
        11,
        31,
        97,
        239,
        491,
        1193,
        4273
    };

    public HierarchyGenerator(SharedCounterGenerator sharedGen) {
        this(sharedGen, PATH_DELIM);
    }

    public HierarchyGenerator(SharedCounterGenerator sharedGen, String delim) {
        super(new String[]{});

        this.idGenerator = sharedGen;
        this.delim = delim;
    }

    /**
     * Build a hierarchy generator that is capable of generating a hierarchy path
     * upto maxDepth and capable to generating upto totalRecords
     *
     * @param maxDepth the max depth of the hierarchy
     * @param totalRecords that needs to be generated
     * @return the root hierarchy generator object
     */
    static public HierarchyGenerator build(int maxDepth, int totalRecords) {

        HierarchyGenerator current = null;
        HierarchyGenerator root = null;

        AtomicLong id = new AtomicLong();
        for(int i = 0; i < maxDepth; i++) {
            HierarchyGenerator gen = new HierarchyGenerator(new SharedCounterGenerator(id));
            if(current != null) {
                current.linkChild(gen);
            } else {
                root = gen;
            }
            current = gen;
        }

        if(maxDepth < 1) {
            throw new RuntimeException("The number of levels desired should be greater than 0");
        }

        if(maxDepth == 1) {
            root.setMaxInLevel(totalRecords);
        } else {
            int chosen = -1;
            for(int i = 0; i < PRIMES.length; i++) {
                int value = PRIMES[i];

                if(Math.pow(value, maxDepth) > totalRecords) {
                    chosen = i;
                    break;
                }
            }
            //System.out.println(String.format("total: %s, depth: %s, chosen: %s, result: %s", totalRecords, maxDepth, chosen, Math.pow(PRIMES[chosen], maxDepth)));

            if(chosen == -1) {
                throw new RuntimeException("Unable to choose a prime value for the desired levels and records."
                + "Consider decreasing the records or increasing the levels.");
            }

            current = root;
            while(current != null) {
                current.setMaxInLevel(PRIMES[chosen]);
                current = current.childGenerator;
            }
        }
        root.setTotal(totalRecords);

        return root;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setMaxInLevel (int maxItems)
    {
        this.maxInLevel = maxItems;
    }

    public void linkChild(HierarchyGenerator child) {
        this.childGenerator = child;
        child.parentGenerator = this;
    }

    @Override public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;
    }

    @Override public boolean hasNext ()
    {
        return (counter < maxInLevel || (childGenerator != null && childGenerator.hasNext())) && (this.parentGenerator != null || invocationCount < total);
    }

    @Override public HierarchyGenerator next ()
    {
        boolean incremented = false;
        if(pathPart == null || childGenerator == null || !childGenerator.hasNext()) {
            generate();

            if(childGenerator != null) {
                childGenerator.reset();
            }

            incremented = true;
            current = this;
        }

        if(!incremented) {
            // childGenerator cannot be null
            current = childGenerator.next();
        }
        invocationCount++;

        notifyListeners(invocationCount, this.visitor);

        return current;
    }

    private void generate() {
        // Can customize the string length later
        pathPart = StringType.randomAlphanumeric(15);

        // Generate the id
        idGenerator.next();

        counter++;
    }

    public void reset() {
        counter = 0;
        pathPart = null;
    }

    public String getPathPart() {
        return this.pathPart;
    }

    public SharedCounterGenerator getIdGenerator() {
        return this.idGenerator;
    }

    public SharedCounterGenerator getCurrentIdGenerator() {
        return current.idGenerator;
    }

    public HierarchyGenerator getParentGenerator() {
        return this.parentGenerator;
    }

    public HierarchyGenerator getCurrentParent() {
        return current.parentGenerator;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        List<String> paths = new ArrayList<>();
        HierarchyGenerator current = this;
        while(current != null) {
            if(current.getPathPart() == null) {
                break;
            }
            paths.add(current.getPathPart());
            current = current.childGenerator;
        }

        StringBuilder sb = new StringBuilder(this.delim);
        sb.append(String.join(this.delim, paths));

        return sb.toString();
    }
}
