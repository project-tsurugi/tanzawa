package com.tsurugidb.console.core.executor.report;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tsubakuro.explain.PlanGraph;
import com.tsurugidb.tsubakuro.explain.PlanNode;

class PlanGraphReporterTest {

    @Test
    void report1() {
        var plan = new PlanGraphTestMock();
        var node = plan.newNode("kind", "title", true);
        node.addAttribute("attr1", "value1");

        String[] expected = { //
                "1. title (kind) {attr1: value1}", //
        };
        testReport(expected, plan);
    }

    @Test
    void report1_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", false);
        node1.addDownstream(node2);

        String[] expected = { //
                "1. title1 (kind1)", //
                "2. title2 (kind2)", //
        };
        testReport(expected, plan);
    }

    @Test
    void report1_1_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", false);
        var node3 = plan.newNode("kind3", "title3", false);
        node1.addDownstream(node2);
        node2.addDownstream(node3);

        String[] expected = { //
                "1. title1 (kind1)", //
                "2. title2 (kind2)", //
                "3. title3 (kind3)", //
        };
        testReport(expected, plan);
    }

    @Test
    void report1_2() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", false);
        var node3 = plan.newNode("kind3", "title3", false);
        node1.addDownstream(node2);
        node1.addDownstream(node3);

        String[] expected = { //
                "1. title1 (kind1) >[1-1, 1-2]", //
                "  1-1. title2 (kind2) <[1]", //
                "  1-2. title3 (kind3) <[1]", //
        };
        testReport(expected, plan);
    }

    @Test
    void report1_1_2() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", false);
        var node3 = plan.newNode("kind3", "title3", false);
        var node4 = plan.newNode("kind4", "title4", false);
        node1.addDownstream(node2);
        node2.addDownstream(node3);
        node2.addDownstream(node4);

        String[] expected = { //
                "1. title1 (kind1)", //
                "2. title2 (kind2) >[2-1, 2-2]", //
                "  2-1. title3 (kind3) <[2]", //
                "  2-2. title4 (kind4) <[2]", //
        };
        testReport(expected, plan);
    }

    @Test
    void report1_2_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", false);
        var node3 = plan.newNode("kind3", "title3", false);
        var node4 = plan.newNode("kind4", "title4", false);
        node1.addDownstream(node2);
        node1.addDownstream(node3);
        node2.addDownstream(node4);
        node3.addDownstream(node4);

        String[] expected = { //
                "1. title1 (kind1) >[1-1, 1-2]", //
                "  1-1. <[1]", //
                "    1-1-1. title2 (kind2)", //
                "    1-1-2. >[2]", //
                "  1-2. <[1]", //
                "    1-2-1. title3 (kind3)", //
                "    1-2-2. >[2]", //
                "2. title4 (kind4) <[1-1-2, 1-2-2]", //
        };
        testReport(expected, plan);
    }

    @Test
    void report1_2_1_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", false);
        var node3 = plan.newNode("kind3", "title3", false);
        var node4 = plan.newNode("kind4", "title4", false);
        var node5 = plan.newNode("kind5", "title5", false);
        node1.addDownstream(node2);
        node1.addDownstream(node3);
        node2.addDownstream(node4);
        node3.addDownstream(node4);
        node4.addDownstream(node5);

        String[] expected = { //
                "1. title1 (kind1) >[1-1, 1-2]", //
                "  1-1. <[1]", //
                "    1-1-1. title2 (kind2)", //
                "    1-1-2. >[2]", //
                "  1-2. <[1]", //
                "    1-2-1. title3 (kind3)", //
                "    1-2-2. >[2]", //
                "2. <[1-1-2, 1-2-2]", //
                "  2-1. title4 (kind4)", //
                "  2-2. title5 (kind5)", //
        };
        testReport(expected, plan);
    }

    @Test
    void report2_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", true);
        var node3 = plan.newNode("kind3", "title3", false);
        node1.addDownstream(node3);
        node2.addDownstream(node3);

        String[] expected = { //
                "1.", //
                "  1-1. title1 (kind1)", //
                "  1-2. >[3]", //
                "2.", //
                "  2-1. title2 (kind2)", //
                "  2-2. >[3]", //
                "3. title3 (kind3) <[1-2, 2-2]", //
        };
        testReport(expected, plan);
    }

    @Test
    void report3_x_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", true);
        var node3 = plan.newNode("kind3", "title3", true);
        var node4 = plan.newNode("kind4", "title4", false);
        var node5 = plan.newNode("kind5", "title5", false);
        node1.addDownstream(node4);
        node2.addDownstream(node4);
        node3.addDownstream(node5);
        node4.addDownstream(node5);

        String[] expected = { //
                "1.", //
                "  1-1. title1 (kind1)", //
                "  1-2. >[4]", //
                "2.", //
                "  2-1. title2 (kind2)", //
                "  2-2. >[4]", //
                "3.", //
                "  3-1. title3 (kind3)", //
                "  3-2. >[5]", //
                "4. <[1-2, 2-2]", //
                "  4-1. title4 (kind4)", //
                "  4-2. >[5]", //
                "5. title5 (kind5) <[3-2, 4-2]", //
        };
        testReport(expected, plan);
    }

    @Test
    void report3_xx_1_1() {
        var plan = new PlanGraphTestMock();
        var node1 = plan.newNode("kind1", "title1", true);
        var node2 = plan.newNode("kind2", "title2", true);
        var node3 = plan.newNode("kind3", "title3", true);
        var node4 = plan.newNode("kind4", "title4", false);
        var node5 = plan.newNode("kind5", "title5", false);
        var node11 = plan.newNode("kind11", "title11", false);
        var node21 = plan.newNode("kind21", "title21", false);
        var node31 = plan.newNode("kind31", "title31", false);
        var node41 = plan.newNode("kind41", "title41", false);
        var node51 = plan.newNode("kind51", "title51", false);
        node1.addDownstream(node11);
        node2.addDownstream(node21);
        node3.addDownstream(node31);
        node4.addDownstream(node41);
        node5.addDownstream(node51);
        node11.addDownstream(node4);
        node21.addDownstream(node4);
        node31.addDownstream(node5);
        node41.addDownstream(node5);

        String[] expected = { //
                "1.", //
                "  1-1. title1 (kind1)", //
                "  1-2. title11 (kind11)", //
                "  1-3. >[4]", //
                "2.", //
                "  2-1. title2 (kind2)", //
                "  2-2. title21 (kind21)", //
                "  2-3. >[4]", //
                "3.", //
                "  3-1. title3 (kind3)", //
                "  3-2. title31 (kind31)", //
                "  3-3. >[5]", //
                "4. <[1-3, 2-3]", //
                "  4-1. title4 (kind4)", //
                "  4-2. title41 (kind41)", //
                "  4-3. >[5]", //
                "5. <[3-3, 4-3]", //
                "  5-1. title5 (kind5)", //
                "  5-2. title51 (kind51)", //
        };
        testReport(expected, plan);
    }

    private static void testReport(String[] expected, PlanGraph plan) {
        var messageList = new ArrayList<String>();
        var reporter = new PlanGraphReporter(message -> messageList.add(message));
        reporter.report("dummy", plan);

        assertLinesMatch(List.of(expected), messageList);
    }

    private static class PlanGraphTestMock implements PlanGraph {

        private final Set<PlanNodeTestMock> sourceSet = new LinkedHashSet<>();

        @Override
        public Set<? extends PlanNode> getNodes() {
            var saw = new LinkedHashSet<PlanNode>(sourceSet);
            var work = new ArrayDeque<PlanNode>(sourceSet);
            while (!work.isEmpty()) {
                var next = work.removeFirst();
                Stream.concat(next.getUpstreams().stream(), next.getDownstreams().stream()) //
                        .filter(it -> !saw.contains(it)) //
                        .forEach(it -> {
                            saw.add(it);
                            work.addFirst(it);
                        });
            }
            return saw;
        }

        @Override
        public Set<? extends PlanNode> getSources() {
            return this.sourceSet;
        }

        @Override
        public Set<? extends PlanNode> getDestinations() {
            throw new UnsupportedOperationException();
        }

        PlanNodeTestMock newNode(String kind, String title, boolean source) {
            var node = new PlanNodeTestMock(kind, title);
            if (source) {
                sourceSet.add(node);
            }
            return node;
        }
    }

    private static class PlanNodeTestMock implements PlanNode {

        private final String kind;
        private final String title;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final Set<PlanNodeTestMock> upstreamSet = new LinkedHashSet<>();
        private final Set<PlanNodeTestMock> downstreamSet = new LinkedHashSet<>();

        PlanNodeTestMock(String kind, String title) {
            this.kind = kind;
            this.title = title;
        }

        @Override
        public String getKind() {
            return this.kind;
        }

        @Override
        public String getTitle() {
            return this.title;
        }

        @Override
        public Map<String, String> getAttributes() {
            return this.attributes;
        }

        @Override
        public Set<? extends PlanNode> getUpstreams() {
            return this.upstreamSet;
        }

        @Override
        public Set<? extends PlanNode> getDownstreams() {
            return this.downstreamSet;
        }

        void addAttribute(String key, String value) {
            attributes.put(key, value);
        }

        void addDownstream(PlanNodeTestMock node) {
            downstreamSet.add(node);
            node.upstreamSet.add(this);
        }

        @Override
        public String toString() {
            return this.title;
        }
    }
}
