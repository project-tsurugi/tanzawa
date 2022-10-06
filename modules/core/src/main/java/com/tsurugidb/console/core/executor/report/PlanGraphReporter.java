package com.tsurugidb.console.core.executor.report;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tsubakuro.explain.PlanGraph;
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
        var rootList = createRootNode(plan, refMap);

        assignNodeId(rootList, refMap);

        for (var node : rootList) {
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

    protected List<ReportNode> createRootNode(PlanGraph plan, Map<PlanNode, ReportNode> refMap) {
        var rawList = plan.getSources();
        var rootList = new ArrayList<ReportNode>(rawList.size());
        for (var planNode : rawList) {
            var node = new ReportNode(planNode, refMap);
            rootList.add(node);
        }
        return rootList;
    }

    protected void assignNodeId(List<ReportNode> rootList, Map<PlanNode, ReportNode> refMap) {
        for (var node : rootList) {
            node.normalizeChildList();
        }

        var seed = new AtomicInteger(0);
        if (rootList.size() == 1 && refMap.isEmpty()) {
            var root = rootList.get(0);
            var nodeId = new NodeId(-1, null, 0);
            root.assignNodeId(nodeId, seed);
        } else {
            for (var node : rootList) {
                var nodeId = new NodeId(0, null, seed.incrementAndGet());
                node.assignNodeId(nodeId, seed);
            }
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
         * @param node   PlanNode
         * @param refMap referenced-node map
         */
        public ReportNode(PlanNode node, Map<PlanNode, ReportNode> refMap) {
            this.planNode = node;
            this.reference = refMap.get(node);
            this.prevList = null;
            initialize(refMap);
        }

        private boolean initialized = false;

        /**
         * initialize.
         *
         * @param refMap referenced-node map
         */
        public void initialize(Map<PlanNode, ReportNode> refMap) {
            if (this.reference != null) {
                reference.addPrev(this);
            }

            if (this.initialized) {
                return;
            }
            this.initialized = true;

            if (this.reference != null) {
                this.nextList = List.of();
                reference.initialize(refMap);
            } else {
                var rawList = planNode.getDownstreams();
                this.nextList = new ArrayList<>(rawList.size());
                for (var rawNext : rawList) {
                    var next = new ReportNode(rawNext, refMap);
                    nextList.add(next);
                }
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
                if (reference.isLast(this)) {
                    reference.normalizeChildList();
                }
                assert nextList.isEmpty();
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
         * @param seed    assign number
         */
        public void assignNodeId(NodeId givenId, AtomicInteger seed) {
            if (this.reference != null) {
                this.nodeId = givenId;

                if (reference.isLast(this)) {
//                  int tab = reference.getPrevList().stream().mapToInt(node -> node.getNodeId().tab()).min().getAsInt();
                    int tab = 0;
                    var nextId = new NodeId(tab, null, seed.incrementAndGet());
                    reference.assignNodeId(nextId, seed);
                }
                return;
            }

            if (this.childList != null) {
                this.groupId = givenId;

                int i = 0;
                this.nodeId = givenId.child(++i);
                for (var child : childList) {
                    var childId = givenId.child(++i);
                    child.assignNodeId(childId, seed);
                }

                assert nextList.isEmpty();
                return;
            }

            this.nodeId = givenId;

            int i = 0;
            for (var next : nextList) {
                var nextId = givenId.child(++i);
                next.assignNodeId(nextId, seed);
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

                if (reference.isLast(this)) {
                    reference.report();
                }
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
         * get group NodeId.
         *
         * @return groupId
         */
        @Nullable
        public NodeId getGroupId() {
            return this.groupId;
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
            prevList.remove(prev);
            prevList.add(prev);
        }

        private boolean isLast(ReportNode node) { // use referenced-node only
            int i = prevList.size() - 1;
            return prevList.get(i) == node;
        }

        /**
         * get previous node list.
         *
         * @return node list
         */
        public List<ReportNode> getPrevList() {
            return this.prevList;
        }
    }

    protected class NodeId {
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
        var idList = prevList.stream().map(n -> n.nodeId.toString()).collect(Collectors.joining(", ", " from [", "]"));
        return idList;
    }
}
