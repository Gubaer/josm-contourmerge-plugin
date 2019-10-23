package org.openstreetmap.josm.plugins.contourmerge;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import lombok.EqualsAndHashCode;

/**
 * <p>A <strong>WaySlice</strong> is a sub sequence of a ways sequence of
 * nodes.</p>
 */
@EqualsAndHashCode(doNotUseGetters = false)
public class WaySlice {
    //static private final Logger logger = Logger.getLogger(WaySlice.class.getName());

    private final Way w;
    private final int start;
    private final int end;
    //NOTE: if 'inDirection==true' this slice doesn't wrap around the end of
    // a closed way; if 'inDirection==false', it does wrap around. Even in
    // the second case start is the lower index, but in this
    // case the slice consists of the nodes
    //      start, start-1,...,0,len-1,len-2,...,end
    private boolean inDirection = true;

    /**
     * <p>Creates a new way slice for the way {@code w}. It consists of the
     * nodes at the positions <code>[start, start+1, ..., end]</code>.</p>
     *
     * @param w the way. Must not be null.
     * @param start the index of the start node. 0 <= start < w.getNodeCount().
     * start < end
     * @param end the index of the end node. 0 <= end < w.getNodeCount().
     * start < end
     * @throws IllegalArgumentException thrown if one of the arguments
     *  isn't valid
     */
    public WaySlice(@NotNull Way w, int start, int end){
        Validate.notNull(w);
        Validate.isTrue(start >= 0 && start < w.getNodesCount(),
                "start out of range, got %s", start);
        Validate.isTrue(end >= 0 && end < w.getNodesCount(),
                "end out of range, got %s", start);
        Validate.isTrue(start < end,
                "expected start < end, got start=%s, end=%s", start, end);
        this.w = w;
        this.start = start;
        this.end = end;
    }

    /**
     * <p>Creates a new way slice for the way {@code w}.</p>
     *
     * <p>If {@code inDirection==true}, it consists of the nodes at the
     * positions <code>[start, start+1, ..., end]</code>.</p>
     *
     * <p>If {@code inDirection==false} <strong>and w is
     * {@link Way#isClosed() closed}</strong>, it consists of the nodes at the
     * positions <code>[end, end+1,...,0,1,...,start]</code>.</p>
     * @param w the way. Must not be null.
     * @param start the index of the start node. 0 <= start < w.getNodeCount().
     *  start < end
     * @param end the index of the end node. 0 <= end < w.getNodeCount().
     * start < end
     * @param inDirection true, this way slice is given by the nodes
     * <code>[start, ..., end]</code>; false, if
     * is  given by the nodes <code>[end,..,0,..,start]</code>
     * (provided the way is closed)
     * @throws IllegalArgumentException thrown if a precondition is violated
     */
    public WaySlice(@NotNull Way w, int start, int end, boolean inDirection){
        this(w,start,end);
        if (!inDirection){
            Validate.isTrue(w.isClosed(),
                 "inDirection=false only supported provided w is closed");
        }
        Validate.isTrue(
            ! (w.isClosed() && start == 0 && end == w.getNodesCount() -1),
            "for a closed way, start and end must not both refer to the "
            + "shared 'join'-node"
        );
        this.inDirection = inDirection;
    }

    /**
     * Replies the way this is a slice of.
     *
     * @return the way this is a slice of.
     */
    public Way getWay() {
        return w;
    }

    /**
     * Replies the index of the first node of this way slice.
     *
     * @return the index of the first node of this way slice
     */
    public int getStart() {
        return start;
    }

    /**
     * Replies the index of the last node of this way slice.
     *
     * @return the index of the first node of this way slice
     */
    public int getEnd() {
        return end;
    }

    /**
     * Replies true, if this way slice has the same direction as the
     * parent way. Replies false, if it has the opposite direction.
     *
     * @return
     */
    public boolean isInDirection() {
        return inDirection;
    }

    public Node getStartNode(){
        return w.getNode(start);
    }

    public Node getEndNode() {
        return w.getNode(end);
    }

    /**
     * <p>Replies the lower node idx of the node from which this way slice is
     * torn off.</p>
     *
     *
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *                    start                       end
     *     ^-- this is the lower position where the way slice [n1,n2,n3] is
     *     torn off
     *     ==> the method replies 0
     * </pre>
     *
     * <p>Replies -1, if there is no such index.</p>
     *
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *     start                         end
     *     The way slice starts at the first node of an open way => there is
     *     no node where the way slice is torn off
     *     ==> the method replies -1
     * </pre>
     *
     * @return
     */
    public int getStartTearOffIdx() {
        if (! w.isClosed()) {  // an open way
            return start > 0 ? start - 1 : -1;
        } else {               // a closed way
            if (isInDirection()){
                int lower = start - 1;
                if (lower < 0) lower = w.getNodesCount() - 2;
                return lower == end ? -1 : lower;
            } else {
                int lower = end - 1;
                if (lower < 0) lower = w.getNodesCount() - 2;
                return lower == start ? -1 : lower;
            }
        }
    }

    /**
     * <p>Replies the lower node from which this way slice is torn off, see
     * {@link #getStartTearOffIdx()} for more details.</p>

     * @return
     */
    public Node getStartTearOffNode() {
        int i = getStartTearOffIdx();
        return i==-1 ? null : w.getNode(i);
    }

    /**
     * <p>Replies the upper node idx of the node from which this way slice is
     * torn off.</p>
     *
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *                    start=====================  end
     *                                                                   ^
     *                                                                   |
     *     this is the upper position where the way slice [n1,n2,n3] is torn off
     *     ==> the method replies 4
     * </pre>
     *
     * <p>Replies -1, if there is no such index.</p>
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *                                    start ===================     end
     *     The way slice ends at the last node of an open way => there is
     *     no node where the way slice is torn off
     *     ==> the method replies -1
     * </pre>
     *
     * @return
     */
    public int getEndTearOffIdx() {
        if (! w.isClosed()) {     // an open way
            return end < w.getNodesCount()-1 ? end + 1 : -1;
        } else {                  // a closed way
            if (inDirection) {
                int upper = end + 1;
                if (upper >= w.getNodesCount()-1) {
                    upper = 0;
                }
                return upper == start ? -1 : upper;
            } else {
                int upper = start + 1;
                if (upper >= w.getNodesCount()-1) {
                    upper = 0;
                }
                return upper == end ? -1 : upper;
            }
        }
    }

    /**
     * <p>Replies the upper node from which this way slice is torn off, see
     * {@link #getEndTearOffIdx()} for more details.</p>

     * @return
     */
    public Node getEndTearOffNode() {
        int i = getEndTearOffIdx();
        return i == -1 ? null : w.getNode(i);
    }

    /**
     * <p>Replies the number of way segments in this way slice.</p>
     *
     * @return the number of way segments in this way slice
     */
    public int getNumSegments() {
        if (inDirection) return end - start;
        return start + (w.getNodesCount() - 1 - end); // for closed ways
    }

    /**
     * <p>Replies the opposite way slice, or null, if this way slice doesn't have
     * an opposite way slice, because it is a way slice in an open way.</p>
     *
     * @return the oposite way slice
     */
    public WaySlice getOpositeSlice(){
        if (!w.isClosed()) return null;
        return new WaySlice(w, start, end, !inDirection);
    }

    /**
     * <p>Replies a clone of the underlying way, where the nodes given by
     * this way slice are replaced with the nodes in {@code newNodes}.</code>
     *
     * @param newNodes the new nodes. Ignored if null.
     * @return the cloned way with the new nodes
     */
    public Way replaceNodes(final List<Node> newNodes) {
        final Way nw = new Way(w);
        if (newNodes == null || newNodes.isEmpty()) return nw;

        if (!w.isClosed()) {
            List<Node> oldNodes = new ArrayList<>(w.getNodes());
            oldNodes.subList(start, end+1).clear();
            oldNodes.addAll(start, newNodes);
            nw.setNodes(oldNodes);
        } else {
            final List<Node> updatedNodeList = new ArrayList<>(w.getNodes());
            if (inDirection) {
                if (start == 0) {
                    updatedNodeList.remove(updatedNodeList.size()-1);
                }
                updatedNodeList.subList(start,end+1).clear();
                updatedNodeList.addAll(start, newNodes);
                if (start == 0) {
                    updatedNodeList.add(newNodes.get(0));
                }
                nw.setNodes(updatedNodeList);
            } else {
                int upper = updatedNodeList.size()-1;
                updatedNodeList.subList(end, upper+1).clear();
                updatedNodeList.subList(0,start+1).clear();
                updatedNodeList.addAll(0, newNodes);
                // make sure the new way is closed
                updatedNodeList.add(newNodes.get(0));
                nw.setNodes(updatedNodeList);
            }
        }
        return nw;
    }

    /**
     * <p>Replies the list of nodes, always starting at the start index,
     * following the nodes in the appropriate direction to the end index.</p>
     *
     * @return the list of nodes
     */
    public List<Node> getNodes(){
        List<Node> nodes = new ArrayList<>();
        if (!w.isClosed()) {
            nodes.addAll(w.getNodes().subList(start, end+1));
        } else {
            if (inDirection) {
                nodes.addAll(w.getNodes().subList(start, end+1));
            } else {
                // do not add the last node which is the join node common
                // to the node at index 0
                for (int i=end; i<=w.getNodesCount()-2;i++) {
                    nodes.add(w.getNode(i));
                }
                for (int i=0; i <= start; i++) nodes.add(w.getNode(i));
            }
        }
        return nodes;
    }


    /**
     * Replies true if this way slice participates in at least one sling.
     * Here's an example of such a sling.
     * <pre>
     *                  5
     *                  |
     *                  |
     *    1======2======3=======4
     *                  |       |
     *                  |       |
     *                  7-------6
     * </pre>
     * <ul>
     *   <li>the nodes [3,4,6,7] form a sling in the way. Note that the way
     *   itself is not <em>closed</em>,
     *   {@link Way#isClosed() isClosed()} will return false.</li>
     *   <li>the way slice [1,2,3,4] <strong>does</strong> participate in a
     *   sling</li>
     *   <li>the way slice [1,2] <strong>doesn't</strong> participate in
     *    a sling</li>
     * </ul>
     *
     * @return true if this way slice participates in at least one sling.
     */
    protected boolean hasSlings() {
        Set<Node> nodeSet = new HashSet<>();
        if (w.isClosed()){
            if (isInDirection()) {
                for (int i=start; i<=end; i++){
                    nodeSet.add(w.getNode(i));
                }
            } else {
                /* A way slice including the common start/end node of a closed
                 * way.
                 * Make sure we look only once at the common start/end node.
                 */
                for (int i=start; i > 0 /* don't add the the start node */; i--){
                    nodeSet.add(w.getNode(i));
                }
                for (int i=w.getNodesCount()-1 /* add the end node */;
                        i >= end; i--){
                    nodeSet.add(w.getNode(i));
                }
            }
        } else {
            for (int i=start; i<=end; i++){
                nodeSet.add(w.getNode(i));
            }
        }
        /*
         * make sure each node in  way slice occurs exactly once in the way.
         * This ensures that the way slice is not participating in any slings.
         */
        Set<Node> seen = new HashSet<>();
        for (int i=0; i< (w.isClosed() ? w.getNodesCount()-1 :
            w.getNodesCount()); i++) {
            Node n = w.getNode(i);
            if (seen.contains(n)) return true;
            if (nodeSet.contains(n)) seen.add(n);
        }
        return false;
    }

    public static class SliceBoundary {
        public SliceBoundary(int start, int end) {
            this.start = start;
            this.end = end;
        }
        int start;
        int end;

        SliceBoundary normalized(int wayLength) {
            return new SliceBoundary(
                start % wayLength,
                end >= wayLength ? (end + 1) % wayLength : end
            );
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<slice-boundary ")
                .append(", start=").append(start)
                .append(", end=").append(end)
                .append(">");
            return sb.toString();
        }
    }

    /**
     * Checks whether a list of way nodes includes the sub sequence
     * of nods in <code>sliceNodes</code>. If yes, replies the
     * {@link SliceBoundary}, i.e. the start and end index of the sub
     * sequence
     *
     * @param wayNodes   the way nodes where we look for a sub sequence.
     *                   Must not be null. At least 2 nodes required.
     * @param sliceNodes the sub sequence. Mot not be nul. At least 2
     *                   nodes required.
     */
    public static Optional<SliceBoundary> findSliceBoundary(
            @NotNull List<Node> wayNodes,
            @NotNull List<Node> sliceNodes) {
        Validate.isTrue(wayNodes.size() >= 2);
        Validate.isTrue(sliceNodes.size() >= 2);

        int start = 0;
        while (start < wayNodes.size()) {
            if (wayNodes.get(start).equals(sliceNodes.get(0))) break;
            start ++;
        }
        if (start >= wayNodes.size()) {
            return Optional.empty();
        }
        int j=0;
        while( j < sliceNodes.size() && start + j < wayNodes.size()) {
            if (!wayNodes.get(start +j).equals(sliceNodes.get(j))) break;
            j++;
        }
        if (j >= sliceNodes.size()) {
            return Optional.of(new SliceBoundary(start, start+j-1));
        }
        return Optional.empty();
    }

    private static Optional<WaySlice> buildWaySliceFromOpenWay(
        @NotNull Way way, @NotNull List<Node> sliceNodes) {
        Validate.isTrue(!way.isClosed());

        Function<SliceBoundary,WaySlice> buildWaySlice = b ->
            new WaySlice(way, b.start, b.end);

        List<Node> wayNodes = way.getNodes();
        Optional<SliceBoundary> boundary = findSliceBoundary(
                wayNodes, sliceNodes);
        if (boundary.isPresent()) {
            return boundary.map(buildWaySlice);
        }
        List<Node> reversedSlice = new ArrayList<>(sliceNodes);
        Collections.reverse(reversedSlice);
        return findSliceBoundary(wayNodes, reversedSlice)
            .map(buildWaySlice);
    }

    private static Optional<WaySlice> buildWaySliceFromClosedWay(
        @NotNull Way way, @NotNull List<Node> sliceNodes) {
        Validate.isTrue(way.isClosed());

        Function<SliceBoundary,WaySlice> buildWaySlice = b -> {
            b = b.normalized(way.getNodesCount());
            if (b.start < b.end) {
                return new WaySlice(way, b.start, b.end);
            } else {
                // b.end < b.start => slice wraps around joint node
                // => set inDirection = false
                return new WaySlice(
                    way, b.end, b.start, false /* inDirection = false */);
            }
        };

        List<Node> wayNodes = way.getNodes().subList(0,way.getNodesCount()-1);
        wayNodes.addAll(way.getNodes());
        Optional<SliceBoundary> boundary = findSliceBoundary(
            wayNodes, sliceNodes);
        if (boundary.isPresent()) {
            return boundary.map(buildWaySlice);
        }
        List<Node> reversedSlice = new ArrayList<>(sliceNodes);
        Collections.reverse(reversedSlice);
        return findSliceBoundary(wayNodes, reversedSlice)
                .map(buildWaySlice);
    }

    /**
     * Builds a way slice for a way consisting of the sequence of nodes in
     * sequence, provided there is such a sequence of nodes in the other way.
     * If not, replies {@link Optional<WaySlice>#empty()}
     *
     * @param way  the way. must not be null. At least two nodes required
     * @param sequence the sequence must not be null. At least two nodes
     *                 required in the sequence
     * @return the way slice
     * @exception NullPointerException if way is null or sequence is null
     * @exception IllegalArgumentException if way has less than 2 nodes
     * @exception IllegalArgumentException if sequence has less than 2 nodes
     */
    public static Optional<WaySlice> buildWaySlice(
            @NotNull Way way,
            @NotNull List<Node> sequence) {

        Validate.isTrue(sequence.size() >= 2);
        Validate.isTrue(way.getNodesCount() >= 2);

        if (!way.isClosed()) {
            return buildWaySliceFromOpenWay(way, sequence);
        } else {
            return buildWaySliceFromClosedWay(way, sequence);
        }
    }

    /**
     * Replies a way slice in another way with the same node sequence
     * as this way slice, provided there is such a node sequence in
     * the other way
     *
     * @param other the other way
     * @return the way slice
     */
    public Optional<WaySlice> asSliceIn(@NotNull Way other) {
        return buildWaySlice(other, getNodes());
    }

    /**
     * Given this way slice, finds all other ways which include the same
     * sequence of nodes as this way slice
     *
     * @return a stream of all  the ways slices
     */
    public Stream<WaySlice> findAllEquivalentWaySlices() {
        return getStartNode().getParentWays().stream()
            .map(candidate -> asSliceIn(candidate))
            .filter(Optional::isPresent)
            .map(candidate -> candidate.get());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<way-slice ").append("way=").append(w.getPrimitiveId())
            .append(", start=").append(start)
            .append(", end=").append(end)
            .append(", isInDirection=").append(isInDirection())
            .append(">");
        return sb.toString();
    }
}
