package tools.xor.logic;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.Vertex;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.DirectedSparseGraph;

/**
 * Test case for dispatcher testing
 *   SerialDispatcher
 *   ParallelDispatcher
 */
public class DefaultDispatcher
{
    private Integer serialTimeTaken; // In milliseconds
    private Integer parallelTimeTaken; // In milliseconds

    private static Map<String, Integer> values = new HashMap<>();
    private final static int QUERY_POOL_SIZE;

    static {
        if (ApplicationConfiguration.config().containsKey(Constants.Config.QUERY_POOL_SIZE)) {
            QUERY_POOL_SIZE = ApplicationConfiguration.config().getInt(Constants.Config.QUERY_POOL_SIZE);
        }
        else {
            QUERY_POOL_SIZE = 10;
        }

        values.put("A", 2);
        values.put("B", 4);
        values.put("C", 4);
        values.put("D", 4);
        values.put("E", 4);
        values.put("F", 4);
        values.put("G", 4);
        values.put("H", 4);
        values.put("C1", 6);
        values.put("C2", 6);
        values.put("C3", 6);
        values.put("F1", 6);
        values.put("F2", 6);
        values.put("G1", 6);
        values.put("H1", 6);
        values.put("C21", 8);
        values.put("G11", 8);
        values.put("G111", 10);
        values.put("G112", 10);
    }

    private static ExecutorService qe = Executors.newFixedThreadPool(QUERY_POOL_SIZE);

    public static class TestNode implements Vertex  {
        private int value = 1;
        private String name;

        public TestNode(String name) {
            this.name = name;
        }

        @Override public String getName ()
        {
            return name;
        }

        @Override public String getDisplayName ()
        {
            return name;
        }

        public int getValue() {
            return this.value;
        }

        public void setValue(int v) {
            this.value = v;
        }
    }

    private static DirectedGraph<TestNode, Edge> buildTree() {
        // Root node
        TestNode nodeA = new TestNode("A");

        // children
        TestNode nodeB = new TestNode("B");
        TestNode nodeC = new TestNode("C");
        TestNode nodeD = new TestNode("D");
        TestNode nodeE = new TestNode("E");
        TestNode nodeF = new TestNode("F");
        TestNode nodeG = new TestNode("G");
        TestNode nodeH = new TestNode("H");

        // grand children
        TestNode nodeC1 = new TestNode("C1");
        TestNode nodeC2 = new TestNode("C2");
        TestNode nodeC3 = new TestNode("C3");
        TestNode nodeF1 = new TestNode("F1");
        TestNode nodeF2 = new TestNode("F2");
        TestNode nodeG1 = new TestNode("G1");
        TestNode nodeH1 = new TestNode("H1");

        // grand children of children
        TestNode nodeC21 = new TestNode("C21");
        TestNode nodeG11 = new TestNode("G11");

        // grand children of grand children
        TestNode nodeG111 = new TestNode("G111");
        TestNode nodeG112 = new TestNode("G112");


        // Create the edges
        DirectedGraph<TestNode, Edge> dg = new DirectedSparseGraph<>();
        dg.addEdge(new Edge("e1", nodeA, nodeB), nodeA, nodeB);
        dg.addEdge(new Edge("e2", nodeA, nodeC), nodeA, nodeC);
        dg.addEdge(new Edge("e3", nodeA, nodeD), nodeA, nodeD);
        dg.addEdge(new Edge("e4", nodeA, nodeE), nodeA, nodeE);
        dg.addEdge(new Edge("e5", nodeA, nodeF), nodeA, nodeF);
        dg.addEdge(new Edge("e6", nodeA, nodeG), nodeA, nodeG);
        dg.addEdge(new Edge("e7", nodeA, nodeH), nodeA, nodeH);

        dg.addEdge(new Edge("e21", nodeC, nodeC1), nodeC, nodeC1);
        dg.addEdge(new Edge("e22", nodeC, nodeC2), nodeC, nodeC2);
        dg.addEdge(new Edge("e23", nodeC, nodeC3), nodeC, nodeC3);

        dg.addEdge(new Edge("e51", nodeF, nodeF1), nodeF, nodeF1);
        dg.addEdge(new Edge("e52", nodeF, nodeF2), nodeF, nodeF2);

        dg.addEdge(new Edge("e61", nodeG, nodeG1), nodeG, nodeG1);

        dg.addEdge(new Edge("e71", nodeH, nodeH1), nodeH, nodeH1);

        dg.addEdge(new Edge("e221", nodeC2, nodeC21), nodeC2, nodeC21);

        dg.addEdge(new Edge("e611", nodeG1, nodeG11), nodeG1, nodeG11);

        dg.addEdge(new Edge("e6111", nodeG11, nodeG111), nodeG11, nodeG111);
        dg.addEdge(new Edge("e6112", nodeG11, nodeG112), nodeG11, nodeG112);

        return dg;
    }

    private void validate(DirectedGraph<TestNode, Edge> dg) {
        for(TestNode node: dg.getVertices()) {
            assert(node.getValue() == values.get(node.getName()));
        }
    }

    private static void process(TestNode node, TestNode parent) throws InterruptedException
    {
        node.setValue(node.getValue() + 1 + (parent != null? parent.getValue():0));
        Thread.sleep(1000); // sleep for a second
    }

    //@Test
    public void testSerial() throws InterruptedException
    {
        DirectedGraph<TestNode, Edge> dg = buildTree();

        long start = (new Date()).getTime();

        // Perform the logic using BFS
        Queue<TestNode> queue = new LinkedList<>();
        queue.addAll(dg.getRoots());

        while(!queue.isEmpty()) {
            TestNode node = queue.remove();

            Iterator iter = dg.getInEdges(node).iterator();
            TestNode parent = null;
            if(iter.hasNext()) {
                parent = (TestNode)((Edge)iter.next()).getStart();
            }
            process(node, parent);

            for(Edge<TestNode> edge: dg.getOutEdges(node)) {
                queue.add(edge.getEnd());
            }
        }

        serialTimeTaken = (int)((new Date()).getTime() - start);
        System.out.println("Serial dispatcher time: " + serialTimeTaken);

        assert(serialTimeTaken >= 19000); // should be equal or greater than 19 seconds

        if(parallelTimeTaken != null) {
            assert(serialTimeTaken > parallelTimeTaken);
        }

        validate(dg);
    }

    private static class NodeProcessor implements Runnable {

        private final TestNode node;
        private final TestNode parent;

        public NodeProcessor(TestNode node, TestNode parent) {
            this.node = node;
            this.parent = parent;
        }

        @Override public void run ()
        {
            try {
                process(node, parent);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Callback {
        private DirectedGraph<TestNode, Edge> dg;
        private CountDownLatch latch;

        public Callback(DirectedGraph<TestNode, Edge> dg) {
            this.dg = dg;
            this.latch = new CountDownLatch(dg.getVertices().size());
        }

        public void process() {
            for(TestNode node: dg.getRoots()) {
                CallbackTask ct = new CallbackTask(new NodeProcessor(node, null), this, node);
                qe.submit(ct);
            }

            try {
                latch.await();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void complete(TestNode node) {


            latch.countDown();

            for(Edge<TestNode> edge: dg.getOutEdges(node)) {
                TestNode child = edge.getEnd();
                CallbackTask ct = new CallbackTask(new NodeProcessor(child, node), this, child);
                qe.submit(ct);
            }
        }

        public DirectedGraph<TestNode, Edge> getTree() {
            return dg;
        }
    }

    private static class CallbackTask implements Runnable {

        private final Runnable task;
        private final Callback callback;
        private final TestNode node;

        CallbackTask(Runnable task, Callback callback, TestNode node) {
            this.task = task;
            this.callback = callback;
            this.node = node;
        }

        public void run() {
            task.run();
            callback.complete(node);
        }

    }

    @Test
    public void testParallel() {
        Callback cb = new Callback(buildTree());

        long start = (new Date()).getTime();
        cb.process();
        parallelTimeTaken = (int)((new Date()).getTime() - start);
        System.out.println("Parallel dispatcher time: " + parallelTimeTaken);

        assert(parallelTimeTaken < 10000); // should easily be less than 10 seconds

        if(serialTimeTaken != null) {
            assert(serialTimeTaken > parallelTimeTaken);
        }

        validate(cb.getTree());
    }
}
