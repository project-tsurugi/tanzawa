package com.tsurugidb.console.core.executor.report;

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
        var refMap = createRefMap(plan);
        var comparator = createComparator(plan);
        var rootList = createRootNode(plan, refMap, comparator);

        var nodeList = new ArrayList<ReportNode>(rootList.size() + refMap.size());
        nodeList.addAll(rootList);
        nodeList.addAll(refMap.values());
        nodeList.sort(comparator);

        assignNodeId(nodeList);

        for (var node : nodeList) {
            node.report();
        }
    }

    protected Map<PlanNode, ReportNode> createRefMap(PlanGraph plan) {
        var refMap = new IdentityHashMap<PlanNode, ReportNode>();
        for (var planNode : plan.getNodes()) {
            if (planNode.getUpstreams().size() >= 2) {
                var node = new ReportNode(planNode);
                refMap.put(planNode, node);
            }
        }
        return refMap;
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

    protected List<ReportNode> createRootNode(PlanGraph plan, Map<PlanNode, ReportNode> refMap, Comparator<ReportNode> comparator) {
        var rawList = plan.getSources();
        var rootList = new ArrayList<ReportNode>(rawList.size());
        for (var planNode : rawList) {
            var node = new ReportNode(planNode, refMap, comparator);
            rootList.add(node);
        }
        return rootList;
    }

    protected void assignNodeId(List<ReportNode> nodeList) {
        for (var node : nodeList) {
            node.normalizeChildList();
        }

        if (nodeList.size() == 1) {
            var root = nodeList.get(0);
            if (root.hasChild()) {
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

    protected class ReportNode {
        private final PlanNode planNode;
        private final ReportNode reference;
        private final List<ReportNode> prevList; // use referenced-node only
        private List<ReportNode> nextList;
        private List<ReportNode> childList;
        private NodeId groupId;
        private NodeId nodeId;

        /**
         * Creates a new instance for referenced-node.
         *
         * @param node PlanNode
         */
        public ReportNode(PlanNode node) {
            this.planNode = node;
            this.reference = null;
            this.prevList = new ArrayList<>();
        }

        /**
         * Creates a new instance.
         *
         * @param node       PlanNode
         * @param refMap     referenced-node map
         * @param comparator comparator of {@link ReportNode}
         */
        public ReportNode(PlanNode node, Map<PlanNode, ReportNode> refMap, Comparator<ReportNode> comparator) {
            this.planNode = node;
            this.reference = refMap.get(node);
            this.prevList = null;

            if (reference != null) {
                reference.addPrev(this);
            }
            initialize(refMap, comparator);
        }

        private boolean initialized = false;

        /**
         * initialize.
         *
         * @param refMap     referenced-node map
         * @param comparator comparator of {@link ReportNode}
         */
        public void initialize(Map<PlanNode, ReportNode> refMap, Comparator<ReportNode> comparator) {
            if (this.initialized) {
                return;
            }
            this.initialized = true;

            if (this.reference != null) {
                this.nextList = List.of();
                reference.initialize(refMap, comparator);
            } else {
                var rawList = planNode.getDownstreams();
                this.nextList = new ArrayList<>(rawList.size());
                for (var rawNext : rawList) {
                    var next = new ReportNode(rawNext, refMap, comparator);
                    nextList.add(next);
                }
                nextList.sort(comparator);
            }
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
         * initialize childList.
         */
        public void normalizeChildList() {
            if (this.reference != null) {
                return;
            }

            if (nextList.size() == 1) {
                var next = nextList.get(0);
                this.nextList = List.of();

                this.childList = new ArrayList<>();
                childList.add(next);

                next.normalizeChildList();
                if (next.childList != null) {
                    childList.addAll(next.childList);
                    next.childList = null;
                }

                return;
            }

            for (var next : nextList) {
                next.normalizeChildList();
            }
        }

        /**
         * assign nodeId.
         *
         * @param givenId NodeId
         */
        public void assignNodeId(NodeId givenId) {
            if (this.reference != null) {
                this.nodeId = givenId;
                return;
            }

            if (this.childList != null) {
                this.groupId = givenId;

                int i = 0;
                this.nodeId = givenId.child(++i);
                for (var child : childList) {
                    var childId = givenId.child(++i);
                    child.assignNodeId(childId);
                }

                assert nextList.isEmpty();
                return;
            }

            this.nodeId = givenId;

            int i = 0;
            for (var next : nextList) {
                var nextId = givenId.child(++i);
                next.assignNodeId(nextId);
            }
        }

        /**
         * displays this node.
         */
        public void report() {
            boolean reportPrevId = true;
            if (this.groupId != null) {
                boolean reported = reportGroup(groupId, this);
                if (reported) {
                    reportPrevId = false;
                }
            }

            if (this.reference != null) {
                reportRefPlanNode(nodeId, this, reference, reportPrevId);
                return;
            }

            reportPlanNode(nodeId, this, reportPrevId);

            if (this.childList != null) {
                for (var child : childList) {
                    child.report();
                }
                assert nextList.isEmpty();
                return;
            }

            for (var next : nextList) {
                next.report();
            }
        }

        /**
         * whether childList is empty.
         *
         * @return {@code true} if childList exists
         */
        public boolean hasChild() {
            return childList != null;
        }

        /**
         * get group NodeId.
         *
         * @return groupId
         */
        @Nullable
        public NodeId getGroupId() {
            return this.groupId;
        }

        /**
         * get nodeId.
         *
         * @return nodeId
         */
        @Nullable
        public NodeId getNodeId() {
            return this.nodeId;
        }

        /**
         * get group NodeId or nodeId.
         *
         * @return groupId or nodeId
         */
        @Nonnull
        public NodeId getGroupOrNodeId() {
            if (this.groupId != null) {
                return this.groupId;
            }
            return this.nodeId;
        }

        private void addPrev(ReportNode prev) { // use referenced-node only
            assert this.prevList != null;
            prevList.add(prev);
        }

        /**
         * get previous node list.
         *
         * @return node list
         */
        public List<ReportNode> getPrevList() {
            return this.prevList;
        }

        @Override
        public String toString() {
            if (this.prevList != null) {
                return planNode + "(ref)";
            }

            return planNode.toString();
        }
    }

    protected class NodeId implements Comparable<NodeId> {
        private final int tab;
        private final String nodeIdText;

        /**
         * Creates a new instance.
         *
         * @param tab    tab
         * @param prevId previous NodeId
         * @param number assign number
         */
        public NodeId(int tab, String prevId, int number) {
            this.tab = tab;
            this.nodeIdText = getNodeId(prevId, number);
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

    protected String getNodeId(String parentId, int number) {
        if (parentId == null || parentId.equals("0")) {
            return Integer.toString(number);
        } else {
            return parentId + "-" + number;
        }
    }

    protected boolean reportGroup(NodeId groupId, ReportNode node) {
        if (groupId.tab() < 0) {
            return false;
        }

        String tab = getTabText(groupId.tab());
        String fromText = getPrevIdText(node, true);
        var message = MessageFormat.format("{0}{1}.{2}", tab, groupId, fromText);
        messageReporter.accept(message);
        return true;
    }

    protected void reportPlanNode(NodeId nodeId, ReportNode node, boolean reportPrevId) {
        var planNode = node.getPlanNode();
        String tab = getTabText(nodeId.tab());
        String title = planNode.getTitle();
        String kind = planNode.getKind();
        String attributes = getPlanNodeAttributesText(planNode);
        String fromText = getPrevIdText(node, reportPrevId);
        var message = MessageFormat.format("{0}{1}. {2} ({3}){4}{5}", tab, nodeId, title, kind, attributes, fromText);
        messageReporter.accept(message);
    }

    protected void reportRefPlanNode(NodeId nodeId, ReportNode node, ReportNode reference, boolean reportPrevId) {
        String tab = getTabText(nodeId.tab());
        var refId = reference.getGroupOrNodeId();
        String fromText = getPrevIdText(node, reportPrevId);
        var message = MessageFormat.format("{0}{1}. goto {2}{3}", tab, nodeId, refId, fromText);
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

    protected String getPlanNodeAttributesText(PlanNode node) {
        Map<String, String> attributes = node.getAttributes();
        if (attributes.isEmpty()) {
            return "";
        }

        var result = new ArrayList<String>(attributes.size());
        attributes.forEach((key, value) -> {
            var text = String.format("%s: %s", key, value);
            result.add(text);
        });
        return " {" + String.join(", ", result) + "}";
    }

    protected String getPrevIdText(ReportNode node, boolean report) {
        if (!report) {
            return "";
        }

        var prevList = node.getPrevList();
        if (prevList == null) {
            return "";
        }
        var idText = prevList.stream().map(ReportNode::getNodeId).sorted().map(NodeId::toString).collect(Collectors.joining(", ", " from [", "]"));
        return idText;
    }
}
