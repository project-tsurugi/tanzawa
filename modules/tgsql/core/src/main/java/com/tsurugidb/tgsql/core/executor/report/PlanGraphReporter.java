/*
 * Copyright 2023-2025 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.tgsql.core.executor.report;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tsubakuro.explain.PlanGraph;
import com.tsurugidb.tsubakuro.explain.PlanGraphUtil;
import com.tsurugidb.tsubakuro.explain.PlanNode;

/**
 * Reporter of {@link PlanGraph}.
 */
public class PlanGraphReporter {

    /** message reporter. */
    protected final Consumer<String> messageReporter;

    /**
     * Creates a new instance.
     *
     * @param messageReporter output message function
     */
    public PlanGraphReporter(@Nonnull Consumer<String> messageReporter) {
        this.messageReporter = messageReporter;
    }

    /**
     * displays execution plan.
     *
     * @param source the source program
     * @param plan   the inspected plan
     */
    public void report(@Nonnull String source, @Nonnull PlanGraph plan) {
        var refMap = new IdentityHashMap<PlanNode, ReportNode>();
        var rootList = createRootNode(plan, refMap);

        var nodeList = new ArrayList<ReportNode>(rootList.size() + refMap.size());
        nodeList.addAll(rootList);
        nodeList.addAll(refMap.values());
        var comparator = createComparator(plan);
        nodeList.sort(comparator);

        assignNodeId(nodeList);

        for (var node : nodeList) {
            node.report();
        }
    }

    protected List<ReportNode> createRootNode(PlanGraph plan, Map<PlanNode, ReportNode> refMap) {
        var rawList = plan.getSources();
        var rootList = new ArrayList<ReportNode>(rawList.size());
        for (var planNode : rawList) {
            var node = createReportNode(planNode, refMap);
            rootList.add(node);
        }
        return rootList;
    }

    protected Comparator<ReportNode> createComparator(PlanGraph plan) {
        var sortedRawList = PlanGraphUtil.sort(plan.getNodes());

        var orderMap = new IdentityHashMap<PlanNode, Integer>(sortedRawList.size());
        int order = 0;
        for (var planNode : sortedRawList) {
            orderMap.put(planNode, order++);
        }

        return (node1, node2) -> {
            int order1 = orderMap.get(node1.getPlanNode());
            int order2 = orderMap.get(node2.getPlanNode());
            return Integer.compare(order1, order2);
        };
    }

    protected void assignNodeId(List<ReportNode> nodeList) {
        if (nodeList.size() == 1) {
            var root = nodeList.get(0);
            if (root instanceof SeqReportNode) {
                var nodeId = new NodeId(-1, null, 0);
                root.assignNodeId(nodeId);
                return;
            }
        }

        int i = 0;
        for (var node : nodeList) {
            var nodeId = new NodeId(0, null, ++i);
            node.assignNodeId(nodeId);
        }
    }

    // ReportNode

    protected ReportNode createReportNode(PlanNode planNode, Map<PlanNode, ReportNode> refMap) {
        var reference = refMap.get(planNode);
        if (reference == null) {
            if (isReferenceNode(planNode)) {
                reference = createReportNodeMain(planNode, refMap);
                refMap.put(planNode, reference);
            }
        }
        if (reference != null) {
            return new JumpReportNode(planNode, reference);
        }

        return createReportNodeMain(planNode, refMap);
    }

    private static boolean isReferenceNode(PlanNode planNode) {
        var upList = planNode.getUpstreams();
        return upList.size() >= 2;
    }

    protected ReportNode createReportNodeMain(PlanNode planNode, Map<PlanNode, ReportNode> refMap) {
        var downList = planNode.getDownstreams();
        switch (downList.size()) {
        case 0:
            return new LeafReportNode(planNode);
        case 1:
            break;
        default:
            var node = new ParReportNode(planNode);
            for (var down : downList) {
                var next = createReportNode(down, refMap);
                node.addNext(next);
            }
            return node;
        }

        var node = new SeqReportNode(planNode);
        node.addChild(new LeafReportNode(planNode));
        var down = downList.iterator().next();
        var child = createReportNode(down, refMap);
        if (child instanceof SeqReportNode) {
            for (var c : ((SeqReportNode) child).getChildList()) {
                node.addChild(c);
            }
        } else {
            node.addChild(child);
        }
        return node;
    }

    protected abstract static class ReportNode {
        private final PlanNode planNode;
        private NodeId nodeId;
        private List<ReportNode> prevList;

        /**
         * Creates a new instance.
         *
         * @param planNode PlanNode
         */
        public ReportNode(PlanNode planNode) {
            this.planNode = planNode;
        }

        /**
         * get {@link PlanNode}.
         *
         * @return PlanNode
         */
        public PlanNode getPlanNode() {
            return this.planNode;
        }

        /**
         * assign node id.
         *
         * @param givenId node id
         */
        public abstract void assignNodeId(NodeId givenId);

        protected void setNodeId(NodeId nodeId) {
            this.nodeId = nodeId;
        }

        /**
         * get nodeId.
         *
         * @return nodeId
         */
        @Nonnull
        public NodeId getNodeId() {
            return this.nodeId;
        }

        /**
         * add previous node.
         *
         * @param prev previous node
         */
        public void addPrev(ReportNode prev) {
            if (this.prevList == null) {
                this.prevList = new ArrayList<>();
            }
            prevList.add(prev);
        }

        /**
         * get previous node list.
         *
         * @return node list
         */
        @Nullable
        public List<ReportNode> getPrevList() {
            return this.prevList;
        }

        /**
         * get next node list.
         *
         * @return node list
         */
        @Nonnull
        public abstract List<ReportNode> getNextList();

        /**
         * displays this node.
         */
        public abstract void report();

        @Override
        public String toString() {
            if (this.prevList != null) {
                return planNode + "(ref)";
            }

            return planNode.toString();
        }
    }

    protected class ParReportNode extends ReportNode {
        private final List<ReportNode> nextList = new ArrayList<>();

        /**
         * Creates a new instance.
         *
         * @param planNode PlanNode
         */
        public ParReportNode(PlanNode planNode) {
            super(planNode);
        }

        /**
         * add next node.
         *
         * @param next next node
         */
        public void addNext(ReportNode next) {
            nextList.add(next);
            next.addPrev(this);
        }

        @Override
        public void assignNodeId(NodeId givenId) {
            setNodeId(givenId);

            int i = 0;
            for (var next : nextList) {
                var nextId = givenId.child(++i);
                next.assignNodeId(nextId);
            }
        }

        @Override
        public List<ReportNode> getNextList() {
            return this.nextList;
        }

        @Override
        public void report() {
            reportNodeDetail(this);

            for (var next : nextList) {
                next.report();
            }
        }
    }

    protected class SeqReportNode extends ReportNode {
        private final List<ReportNode> childList = new ArrayList<>();

        /**
         * Creates a new instance.
         *
         * @param planNode PlanNode
         */
        public SeqReportNode(PlanNode planNode) {
            super(planNode);
        }

        /**
         * add child node.
         *
         * @param child child node
         */
        public void addChild(ReportNode child) {
            childList.add(child);
        }

        /**
         * get child node list.
         *
         * @return node list
         */
        public List<ReportNode> getChildList() {
            return this.childList;
        }

        @Override
        public void assignNodeId(NodeId givenId) {
            setNodeId(givenId);

            int i = 0;
            for (var child : childList) {
                var childId = givenId.child(++i);
                child.assignNodeId(childId);
            }
        }

        @Override
        public List<ReportNode> getNextList() {
            return List.of();
        }

        @Override
        public void report() {
            reportNodeId(this);

            for (var child : childList) {
                child.report();
            }
        }
    }

    protected class LeafReportNode extends ReportNode {

        /**
         * Creates a new instance.
         *
         * @param planNode PlanNode
         */
        public LeafReportNode(PlanNode planNode) {
            super(planNode);
        }

        @Override
        public void assignNodeId(NodeId givenId) {
            setNodeId(givenId);
        }

        @Override
        public List<ReportNode> getNextList() {
            return List.of();
        }

        @Override
        public void report() {
            reportNodeDetail(this);
        }
    }

    protected class JumpReportNode extends ReportNode {
        private final ReportNode reference;

        /**
         * Creates a new instance.
         *
         * @param planNode  PlanNode
         * @param reference reference node
         */
        public JumpReportNode(PlanNode planNode, ReportNode reference) {
            super(planNode);
            this.reference = reference;

            reference.addPrev(this);
        }

        @Override
        public void assignNodeId(NodeId givenId) {
            setNodeId(givenId);
        }

        @Override
        public List<ReportNode> getNextList() {
            return List.of(reference);
        }

        @Override
        public void report() {
            reportNodeId(this);
        }
    }

    // node id

    protected class NodeId implements Comparable<NodeId> {
        private final int tab;
        private final String nodeIdText;

        /**
         * Creates a new instance.
         *
         * @param tab      tab
         * @param parentId parent NodeId
         * @param number   assign number
         */
        public NodeId(int tab, String parentId, int number) {
            this.tab = tab;
            this.nodeIdText = createNodeId(parentId, number);
        }

        /**
         * Creates a new instance.
         *
         * @param number assign number
         * @return NodeId
         */
        public NodeId child(int number) {
            return new NodeId(tab + 1, toString(), number);
        }

        /**
         * get tab.
         *
         * @return tab
         */
        public int tab() {
            return this.tab;
        }

        @Override
        public String toString() {
            return this.nodeIdText;
        }

        @Override
        public int hashCode() {
            return nodeIdText.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NodeId)) {
                return false;
            }
            var that = (NodeId) obj;
            return nodeIdText.equals(that.nodeIdText);
        }

        @Override
        public int compareTo(NodeId that) {
            String[] ss1 = this.nodeIdText.split("-");
            String[] ss2 = that.nodeIdText.split("-");
            int size = Math.min(ss1.length, ss2.length);
            for (int i = 0; i < size; i++) {
                int n1 = Integer.parseInt(ss1[i]);
                int n2 = Integer.parseInt(ss2[i]);
                int c = Integer.compare(n1, n2);
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(ss1.length, ss2.length);
        }
    }

    protected String createNodeId(String parentId, int number) {
        if (parentId == null || parentId.equals("0")) {
            return Integer.toString(number);
        } else {
            return parentId + "-" + number;
        }
    }

    // report

    protected void reportNodeId(ReportNode node) {
        var nodeId = node.getNodeId();
        if (nodeId.tab() < 0) {
            return;
        }

        String tab = getTabText(nodeId.tab());
        String fromText = getPrevIdText(node);
        String toText = getNextIdText(node);
        var message = MessageFormat.format("{0}{1}.{2}{3}", tab, nodeId, fromText, toText);
        messageReporter.accept(message);
    }

    protected void reportNodeDetail(ReportNode node) {
        var nodeId = node.getNodeId();
        var planNode = node.getPlanNode();
        String tab = getTabText(nodeId.tab());
        String title = planNode.getTitle();
        String kind = planNode.getKind();
        String attributes = getAttributesText(planNode);
        String fromText = getPrevIdText(node);
        String toText = getNextIdText(node);
        var message = MessageFormat.format("{0}{1}. {2} ({3}){4}{5}{6}", tab, nodeId, title, kind, attributes, fromText, toText);
        messageReporter.accept(message);
    }

    protected String getTabText(int tab) {
        if (tab <= 0) {
            return "";
        }

        var tabString = "  ";
        var sb = new StringBuilder(tabString.length() * tab);
        for (int i = 0; i < tab; i++) {
            sb.append(tabString);
        }
        return sb.toString();
    }

    protected String getAttributesText(PlanNode planNode) {
        Map<String, String> attributes = planNode.getAttributes();
        if (attributes.isEmpty()) {
            return "";
        }

        var attrText = attributes.entrySet().stream() //
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue())) //
                .collect(Collectors.joining(", ", " {", "}"));
        return attrText;
    }

    protected String getPrevIdText(ReportNode node) {
        return getIdText(node.getPrevList(), "<");
    }

    protected String getNextIdText(ReportNode node) {
        return getIdText(node.getNextList(), ">");
    }

    protected String getIdText(List<ReportNode> nodeList, String prefix) {
        if (nodeList == null || nodeList.isEmpty()) {
            return "";
        }

        var idText = nodeList.stream() //
                .map(ReportNode::getNodeId).sorted() //
                .map(NodeId::toString) //
                .collect(Collectors.joining(", ", " " + prefix + "[", "]"));
        return idText;
    }
}
