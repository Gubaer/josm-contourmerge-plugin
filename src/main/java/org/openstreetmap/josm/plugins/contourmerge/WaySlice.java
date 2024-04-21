package org.openstreetmap.josm.plugins.contourmerge;

import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.Validate;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * A <strong>WaySlice</strong> is a sub-sequence of a ways sequence of
 * nodes.
 */
@EqualsAndHashCode()
public class WaySlice {
    @SuppressWarnings("unused")
    static private final Logger logger =
        Logger.getLogger(WaySlice.class.getName());

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
     * Creates a new way slice for the way {@code w}. It consists of the
     * nodes at the positions <code>[start, start+1, ..., end]</code>.
     *
     * @param w the way. Must not be null.
     * @param start the index of the start node. 0 <= start < w.getNodeCount().
     * start < end
     * @param end the index of the end node. 0 <= end < w.getNodeCount().
     * start < end
     * @throws IllegalArgumentException thrown if one of the arguments
     *  isn't valid
     */
    public WaySlice(@NotNull final Way w, final int start, final int end){
        Objects.requireNonNull(w);
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
     * Creates a new way slice for the way {@code w}.
     * <p>
     * If {@code inDirection==true}, it consists of the nodes at the
     * positions <code>[start, start+1, ..., end]</code>.
     * <p>
     * If {@code inDirection==false} <strong>and w is
     * {@link Way#isClosed() closed}</strong>, it consists of the nodes at the
     * positions <code>[end, end+1,...,0,1,...,start]</code>.
     *
     * @param w the way. Must not be null.
     * @param start the index of the start node. 0 <= start < w.getNodeCount().
     *  start < end
     * @param end the index of the end node. 0 <= end < w.getNodeCount().
     * start < end
     * @param inDirection true, this way slice is given by the nodes
     * <code>[start, ..., end]</code>; false, if it
     * is given by the nodes <code>[end,..,0,..,start]</code>
     * (provided the way is closed)
     * @throws IllegalArgumentException thrown if a precondition is violated
     */
    public WaySlice(@NotNull final Way w, final int start, final int end,
                    final boolean inDirection){
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
     * @return true, if way slice has the same direction as the parent way
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
     * Replies true if this slice contains the node <code>node</code>
     *
     * @param node the node
     * @return true if this slice contains the node <code>node</code>
     */
    public boolean containsNode(@NotNull final Node node) {
        return getNodes().contains(node);
    }

    /**
     * Replies the lower node idx of the node from which this way slice is
     * torn off.
     * <p>
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *                    start                       end
     *     ^-- this is the lower position where the way slice [n1,n2,n3] is
     *     torn off
     *     ==> the method replies 0
     * </pre>
     *
     * Replies -1, if there is no such index.
     * <p>
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *     start                         end
     *     The way slice starts at the first node of an open way => there is
     *     no node where the way slice is torn off
     *     ==> the method replies -1
     * </pre>
     *
     * @return the start tear-off index
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
     * Replies the lower node from which this way slice is torn off, see
     * {@link #getStartTearOffIdx()} for more details.
     *
     * @return the start tear-off node
     */
    public Node getStartTearOffNode() {
        int i = getStartTearOffIdx();
        return i==-1 ? null : w.getNode(i);
    }

    /**
     * Replies the upper node idx of the node from which this way slice is
     * torn off.
     * <p>
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
     * Replies -1, if there is no such index.
     * <p>
     * <strong>Example</strong>
     * <pre>
     *     n0 ------------- n1 ---------- n2 --------- n3 -------------- n4
     *                                    start ===================     end
     *     The way slice ends at the last node of an open way => there is
     *     no node where the way slice is torn off
     *     ==> the method replies -1
     * </pre>
     *
     * @return the end tear-off index
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
     * Replies the end node from which this way slice is torn off, see
     * {@link #getEndTearOffIdx()} for more details.

     * @return the end tear-off node
     */
    public Node getEndTearOffNode() {
        int i = getEndTearOffIdx();
        return i == -1 ? null : w.getNode(i);
    }

    /**
     * Replies the number of way segments in this way slice.
     *
     * @return the number of way segments in this way slice
     */
    public int getNumSegments() {
        if (inDirection) return end - start;
        return start + (w.getNodesCount() - 1 - end); // for closed ways
    }

    /**
     * Replies the opposite way slice, or null, if this way slice doesn't have
     * an opposite way slice, because it is a way slice in an open way.
     *
     * @return the opposite way slice
     */
    public WaySlice getOppositeSlice(){
        if (!w.isClosed()) return null;
        return new WaySlice(w, start, end, !inDirection);
    }

    private Way replaceNodesInOpenWay(final List<Node> newNodes) {
        final List<Node> updatedNodeList = new ArrayList<>(w.getNodes());
        updatedNodeList.subList(start, end + 1).clear();
        updatedNodeList.addAll(start, newNodes);

        final Way newWay = new Way(w);
        newWay.setNodes(updatedNodeList);
        return newWay;
    }

    private void ensureInvariantsForClosedWay(final List<Node> nodes)
        throws DataIntegrityProblemException {

        if (nodes.size() < 3) {
            throw new DataIntegrityProblemException(tr(
                  "expected >= 3 nodes in a node list for a closed way, "
                + "got {0} nodes",
                nodes.size())
            );
        }

        if (nodes.get(0) != nodes.get(nodes.size() - 1)) {
            throw new DataIntegrityProblemException(tr(
                 "expected identical nodes at positions 0 and {0} in a closed "
                + "way, got different nodes",
                nodes.size() - 1)
            );
        }

        for (int i = 1; i < nodes.size() - 1; i++) {
            if (nodes.get(i) == nodes.get(i - 1)) {
                throw new DataIntegrityProblemException(tr(
                      "identified sequence of equal nodes in a node list, "
                    + "illegal sequence starts a position {0}",
                    i - 1)
                );
            }
        }
    }

    private Way replaceNodesInClosedWay(final List<Node> newNodes)
        throws DataIntegrityProblemException{
        final List<Node> nodes = new ArrayList<>(w.getNodes());

        if (inDirection) {
            // because the slice is 'in direction' either the start node,
            // the end node, neither of them, but not both, are
            // included in the slice.
            if (start == 0) {
                // jn -- n ........   n -- n -- ...-- jn
                // <---- slice     -->
                // <-- cut&replace -->               cut
                //
                // (jn - shared join node; n - arbitrary node)

                // cut the source nodes ...
                nodes.subList(0, end+1).clear();
                nodes.remove(nodes.size() - 1);
                // ...  replace with target nodes ...
                nodes.addAll(0, newNodes);
                // ... and make sure the way is closed again
                nodes.add(0, nodes.get(
                     nodes.size() - 1));
            } else if (end == w.getNodesCount() - 1) {
                // jn -- n ....    n -- .......   -- jn
                //                 <---- slice      -->
                // cut             <-- cut&replace  -->
                //
                // (jn - shared join node; n - arbitrary node)

                // cut the source nodes ...
                nodes.subList(start, end + 1).clear();
                nodes.remove(0);
                // ... replace them with the target nodes ...
                nodes.addAll(newNodes);
                // ... and make sure the way is closed again
                nodes.add(nodes.get(0));
            } else {
                // jn -- n -- n    ....          n -- ... -- jn
                //            <----    slice   -->
                // keep       <-- cut&replace  -->         keep
                //
                // (jn - shared join node; n - arbitrary node)

                // cut the source nodes ...
                nodes.subList(start, end + 1).clear();
                // ...  replace them with the target nodes
                nodes.addAll(start, newNodes);

                // We didn't touch the join node (jn). The updatedNodeList
                // still contains the node list for a closed node.
            }

            // Added after issue-21: Explicitly checks that the list
            // of nodes from which we will build the merged source way doesn't
            // violate expected invariants: minimal length, common start/end
            // node, and no subsequences of identical nodes
            ensureInvariantsForClosedWay(nodes);
        } else {
            int upper = nodes.size()-1;
            nodes.subList(end, upper + 1).clear();
            nodes.subList(0,start + 1).clear();
            nodes.addAll(0, newNodes);
            // make sure the new way is closed
            nodes.add(newNodes.get(0));
        }

        final Way newWay = new Way(w);
        newWay.setNodes(nodes);
        return newWay;
    }

    /**
     * Replies a clone of the underlying way, where the nodes given by
     * this way slice are replaced with the nodes in {@code newNodes}.
     *
     * @param newNodes the new nodes. Ignored if null.
     * @return the cloned way with the new nodes
     */
     public Way replaceNodes(final List<Node> newNodes) {
        if (w.isClosed()) {
            return replaceNodesInClosedWay(newNodes);
        } else {
            return replaceNodesInOpenWay(newNodes);
        }
    }

    /**
     * Replies the list of nodes, always starting at the start index,
     * following the nodes in the appropriate direction to the end index.
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
    @SuppressWarnings("unused")
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
                for (int i=start; i > 0 /* don't add the start node */; i--){
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
            return "<slice-boundary " +
                ", start=" + start +
                ", end=" + end +
                ">";
        }
    }

    /**
     * Checks whether a list of way nodes includes the sub-sequence
     * of nods in <code>sliceNodes</code>. If yes, replies the
     * {@link SliceBoundary}, i.e. the start and end index of the sub-     * sequence
     *
     * @param wayNodes   the way nodes where we look for a sub-sequence.
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
            .map(this::asSliceIn)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    @Override
    public String toString() {
        return "<way-slice " + "way=" + w.getPrimitiveId() +
            ", start=" + start +
            ", end=" + end +
            ", isInDirection=" + isInDirection() +
            ">";
    }
}
