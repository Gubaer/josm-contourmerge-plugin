package org.openstreetmap.josm.plugins.contourmerge;

import org.apache.commons.lang3.Validate;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.data.osm.event.*;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * <strong>ContourMergeModel</strong> keeps the current edit state for a
 * specific edit layer, if the <tt>contourmerge</tt> map mode is enabled.</p>
 */
public class ContourMergeModel implements DataSetListener{
    public static <T extends OsmPrimitive> List<T> getFilteredList(
            Collection<OsmPrimitive> list, Class<T> type) {
        return (list != null ? list.stream() : Stream.empty())
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public static <T extends OsmPrimitive> Set<T> getFilteredSet(
            Collection<OsmPrimitive> set, Class<T> type) {
        return (set != null ? set.stream() : Stream.empty())
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }


    @SuppressWarnings("unused")
    static private final Logger logger =
        Logger.getLogger(ContourMergeModel.class.getName());

    private final OsmDataLayer layer;
    private Node feedbackNode;
    private IWaySegment<Node, Way> dragStartFeedbackSegment;
    private IWaySegment<Node, Way> dropFeedbackSegment;
    private final ArrayList<Node> selectedNodes = new ArrayList<>();
    private Point dragOffset = null;

    /**
     * <p>Creates a new contour merge model for the layer {@code layer}.</p>
     *
     * @param layer the data layer. Must not be null.
     * @throws NullPointerException thrown if {@code layer} is null
     */
    public ContourMergeModel(@NotNull OsmDataLayer layer){
        Validate.notNull(layer);
        this.layer = layer;
    }

    /**
     * <p>Replies the data layer this model operates on.</p>
     *
     * @return the data layer
     */
    public OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * <p>Replies the node the mouse is currently hovering over.</p>
     *
     * @return the node
     */
    public Node getFeedbackNode(){
        return feedbackNode;
    }

    /**
     * <p>Sets the node the mouse is currently hovering over.</p>
     *
     * @param node the node
     */
    public void setFeedbackNode(Node node){
        this.feedbackNode = node;
    }

    public void reset() {
        setFeedbackNode(null);
    }

    /* --------------------------------------------------------------------- */
    /* selecting nodes and way segments                                      */
    /* --------------------------------------------------------------------- */
    /**
     * <p>Replies true, if {@code node} is currently selected in the contour
     *  merge mode.</p>
     *
     * @param node the node. Must not be null. Must be owned by this models
     * layer.
     * @return true, if {@code node} is currently selected in the contour merge
     *  mode.
     */
    public boolean isSelected(@NotNull Node node) {
        Validate.notNull(node);
        Validate.isTrue(node.getDataSet() == layer.data,
            // don't translate
            "Node must be owned by this contour merge models layer");
        return selectedNodes.contains(node);
    }

    /**
     * <p>Selects the node {@code node}.</p>
     *
     * @param node the node. Must not be null. Must be owned by this models
     * layer.
     */
    public void selectNode(@NotNull Node node) {
        Validate.notNull(node);
        Validate.isTrue(node.getDataSet() == layer.data,
            // don't translate
            "Node must be owned by this contour merge models layer");
        if (!isSelected(node)) selectedNodes.add(node);
    }

    /**
     * <p>Deselects the node {@code node}.</p>
     *
     * @param node the node. Must not be null. Must be owned by this models
     *  layer.
     */
    public void deselectNode(@NotNull Node node) {
        Validate.notNull(node);
        Validate.isTrue(node.getDataSet() == layer.data,
            //don't translate
           "Node must be owned by this contour merge models layer");
        selectedNodes.remove(node);
    }

    /**
     * <p>Toggles whether the node {@code node} is selected or not.</p>
     *
     * @param node the node. Must not be null. Must be owned by this models
     *  layer.
     */
    public void toggleSelected(Node node) {
        Validate.notNull(node);
        Validate.isTrue(node.getDataSet() == layer.data,
            // don't translate
           "Node must be owned by this contour merge models layer");
        if (isSelected(node)) {
            deselectNode(node);
        } else {
            selectNode(node);
        }
    }

    /**
     * <p>Deselects all nodes.</p>
     */
    public void deselectAllNodes(){
        selectedNodes.clear();
    }

    /**
     * <p>Replies an <strong>unmodifiable</strong> list of the currently
     * selected nodes.</p>
     *
     * @return an <strong>unmodifiable</strong> list of the currently
     * selected nodes.</p>
     */
    public List<Node> getSelectedNodes() {
        return Collections.unmodifiableList(selectedNodes);
    }

    /**
     * <p>Sets the way segment which would be affected by the next drag/drop
     * operation.</p>
     *
     * @param segment the way segment. null, if there is no feedback
         segment
     */
    public void setDragStartFeedbackWaySegment(IWaySegment<Node, Way> segment){
        this.dragStartFeedbackSegment = segment;
    }

    /**
     * <p>Replies the current feedback way segment or null, if there is
     * currently no such segment
     *
     * @return the feedback way segment
     */
    public IWaySegment<Node, Way> getDragStartFeedbackWaySegement(){
        return dragStartFeedbackSegment;
    }

    public void setDropFeedbackSegment(IWaySegment<Node, Way> segment){
        this.dropFeedbackSegment = segment;
    }

    public IWaySegment<Node, Way> getDropFeedbackSegment(){
        return dropFeedbackSegment;
    }

    /**
     * <p>Replies the set of selected ways, i.e. the set of all parent ways of
     * the selected nodes.</p>
     *
     * @return the set of selected ways
     */
    protected Set<Way> computeSelectedWays(){
        return selectedNodes.stream()
            .flatMap(n -> getFilteredList(
                n.getReferrers(),Way.class
            ).stream())
            .collect(Collectors.toSet());
    }

    /**
     * <p>Replies the set of selected nodes on the way {@code way}.</p>
     *
     * @param way the way
     * @return the set of selected nodes
     */
    protected Set<Node> computeSelectedNodesOnWay(Way way){
        return selectedNodes.stream()
            .filter(n -> getFilteredSet(n.getReferrers(),
                    Way.class).contains(way)
             )
            .collect(Collectors.toSet());
    }

    /**
     * <p>Replies true, if we can start a drag/drop operation on way slice
     * which is given by the currently selected nodes and the way segment
     * {@code ws}.</p>
     *
     *  @return true, if we can start a drag/drop operation. false, otherwise
     */
    public boolean isWaySegmentDragable(IWaySegment<?, Way> ws){
        WaySlice slice = getWaySliceFromSelectedNodes(ws);
        return slice != null;
    }

    /**
     * <p>Replies true, if {@code ws} is part of a potential drop target.</p>
     *
     * @param ws the way segment. If null, replies false.
     * @return  true, if {@code ws} is part of a potential drop target
     */
    public boolean isPotentialDropTarget(IWaySegment<?, Way> ws){
        if (ws == null) return false;
        WaySlice dropTarget = getWaySliceFromSelectedNodes(ws);
        if (dropTarget == null) return false;

        // make sure we don't try to drop on the drag source, not even
        // on a different way slice on the way we drag from
        WaySlice dragSource = getDragSource();
        if (dragSource == null) return true;
        return ! dragSource.getWay().equals(dropTarget.getWay());
    }

    protected List<Integer> computeSelectedNodeIndicesOnWay(Way way){
        return computeSelectedNodesOnWay(way).stream()
            .map(n -> way.getNodes().indexOf(n))
            .sorted()
            .collect(Collectors.toList());
    }

    protected WaySlice getWaySliceFromSelectedNodes(
            IWaySegment<?, Way> referenceSegment){
        if (referenceSegment == null) return null;
        Way way = referenceSegment.getWay();
        if (way == null || way.getNodesCount() == 0) {
            // shouldn't happen, but consistency of a dataset is sometimes
            // violated after undo/redo/merge/etc. operations.
            // This is a workaround for potential defects similar to
            // https://github.com/Gubaer/josm-contourmerge-plugin/issues/4
            return null;
        }
        if (way.isClosed()){
            /*
             * This is a closed way. We need at least two selected nodes to
             * come  up with a way slice.
             */
            List<Integer> selIndices = computeSelectedNodeIndicesOnWay(way);
            if (selIndices.size() <2) return null;

            int nn= way.getNodesCount();
            int li = referenceSegment.getLowerIndex();
            int lower = -1; int upper = nn;
            /*
             * Find the first selected node to the "left" of the way segment,
             * wrapping around at the join-node, if necessary.
             */
            for (int i=li; i>=0;i--){
                if (selIndices.contains(i)) {lower = i; break;}
            }
            if (lower == -1){ // not found yet - wrap around and continue search
                for (int i=nn-1; i>li; i--){
                    if (selIndices.contains(i)) {lower = i; break;}
                }
            }
            /*
             * Find the first selected node to the "right" of the way segment,
             *  wrapping around at the join-node, if necessary.
             */
            for (int i=li+1; i< nn-1 ; i++){
                if (selIndices.contains(i)) {upper = i; break;}
            }
            if (upper == nn){ // not found yet - wrap around and continue search
                for (int i=0; i<li; i++){
                    if (selIndices.contains(i)) {upper = i; break;}
                }
                /*
                 * not really a wrap around? => adjust the index
                 */
                if (upper == 0) upper = nn-1;
            }
            if (lower < upper){
                if (upper == nn -1) {
                    return new WaySlice(way, 0, lower,
                            false /* reverse direction */);
                } else {
                    return new WaySlice(way, lower,upper);
                }
            } else if (lower == upper ){
                return new WaySlice(way, 0, upper,
                        false /* reverse direction */);
            } else {
                return new WaySlice(way, upper, lower,
                        false /* reverse direction */);
            }
        } else {
            /*
             * This is an open way. We can always reply a way slice. If no
             * nodes are selected, we drag the entire way. If 1 node
             * is selected, the way segment determines whether we drag the
             * first or the second half. If more than 1 nodes are selected,
             * we drag the way slice between two selected, or the first or the
             * last node respectively.
             */
            List<Integer> selIndices = computeSelectedNodeIndicesOnWay(
                    referenceSegment.getWay());
            int nn= way.getNodesCount();
            int li = referenceSegment.getLowerIndex();
            int lastPos = nn -1;
            int lower = 0; int upper = lastPos;
            for (int pos=li; pos >=0; pos--){
                if (selIndices.contains(pos)) {lower = pos; break;}
            }
            for (int pos=li+1; pos <=lastPos; pos++){
                if (selIndices.contains(pos)) {upper = pos; break;}
            }
            if (lower == upper) return null;
            return new WaySlice(referenceSegment.getWay(), lower, upper);
        }
    }

    /**
     * <p>Replies the way slice we are currently dragging, or null, if we
     * aren't in a drag operation.</p>
     *
     * @return the way slice or null
     */
    public WaySlice getDragSource(){
        if (dragStartFeedbackSegment == null) return null;
        return getWaySliceFromSelectedNodes(dragStartFeedbackSegment);
    }

    /**
     * <p>Replies the way slice we are currently hovering over and which is
     * suitable as drop target, or null, if no such way slice is currently
     * known.</p>
     *
     * @return the way slice or null
     */
    public WaySlice getDropTarget(){
        if (dropFeedbackSegment == null) return null;
        return getWaySliceFromSelectedNodes(dropFeedbackSegment);
    }

    /**
     * <p>Sets the current drag offset, relative to the point where the
     * drag operation started. Set null to indicate, that there is currently
     * no drag operation. </p>
     *
     * @param offset the drag offset
     */
    public void setDragOffset(Point offset){
        this.dragOffset = offset;
    }

    /**
     * <p>Replies the current drag offset or null, if we aren't in a drag
     *  operation.</p>
     *
     * @return the drag offset
     */
    public Point getDragOffset(){
        return dragOffset;
    }

    /**
     * <p>Replies true, if we are currently in a drag operation.</p>
     *
     * @return true, if we are currently in a drag operation
     */
    public boolean isDragging() {
        return dragOffset != null;
    }


    protected Stream<Command> buildSourceChangeCommands(
            final List<WaySlice> sources,
            final WaySlice target) {

        // The sources are merged to the target. As a consequence, a sequence
        // of nodes in each source is replaced by the same sequence of nodes
        // from the target.
        // The target itself remains unchanged. We have to build a change
        // command for each affected source way.

        final List<Node> targetNodes = target.getNodes();
        final List<Node> targetNodesReversed = new ArrayList<>(targetNodes);
        Collections.reverse(targetNodesReversed);

        return sources.stream().map(source -> {
            final Way modifiedSourceWay;
            if (areDirectionAligned(source, target)) {
                modifiedSourceWay = source.replaceNodes(targetNodes);
            } else {
                modifiedSourceWay = source.replaceNodes(targetNodesReversed);
            }
            return new ChangeCommand(source.getWay(), modifiedSourceWay);
        });
    }

    protected Stream<Command> buildNodeDeleteCommands(
            final List<WaySlice> sources) {
        Validate.isTrue(!sources.isEmpty(),
                // don't translate
                "sources must not be empty");

        // All source ways have the sequence of nodes in common
        // which are replaced by a sequence of the target nodes.

        // In general, all these nodes can and should be deleted
        // after the merge operation, except
        // * if a node is also shared by another way which does not
        //   participate as source in the merge operation
        // * if a node is tagged. We don't want to lose the tags because
        //   of a merge operation

        final WaySlice first = sources.get(0);
        final Set<Way> ways = sources.stream().map(WaySlice::getWay)
                .collect(Collectors.toSet());
        return first.getNodes().stream()
            .map(n -> {
                // true, if the node n is only referenced by source ways
                // in the merge operation
                final boolean hasNoParents = n.getReferrers().stream()
                    .allMatch(ways::contains);

                if (hasNoParents && !n.isTagged()) {
                    return Optional.of(new DeleteCommand(n));
                }
                return Optional.<DeleteCommand>empty();
             })
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    /**
     * <p>Builds the command to align the two contours. Replies null, if the
     * command can't be created, i.e. because there is no defined drag source
     * or drop target.</p>
     *
     * @return the contour align command
     */
    public Command buildContourAlignCommand() {
        final WaySlice dragSource = getDragSource();
        final WaySlice dropTarget = getDropTarget();
        return buildContourAlignCommand(dragSource, dropTarget);
    }

    public @Null Command buildContourAlignCommand(
            @Null final WaySlice dragSource,
            @Null final WaySlice dropTarget) {
        if (dragSource == null || dropTarget == null) return null;

        final List<WaySlice> waySlices =
                dragSource.findAllEquivalentWaySlices()
                        .collect(Collectors.toList());

        final List<Command> cmds = Stream.concat(
                buildSourceChangeCommands(waySlices, dropTarget),
                buildNodeDeleteCommands(waySlices)
        ).collect(Collectors.toList());

        return new SequenceCommand(tr("Merging Contour"), cmds);
    }

    protected boolean haveSameStartAndEndNode(List<Node> n1, List<Node> n2) {
        return
            n1.get(0) == n2.get(0)
         && n1.get(n1.size()-1) == n2.get(n2.size()-1);
    }

    protected boolean haveReversedStartAndEndeNode(List<Node> n1, List<Node> n2) {
        return
            n1.get(0) == n2.get(n2.size()-1)
         && n1.get(n1.size()-1) == n2.get(0);
    }

    /**
     * <p>Replies true, if the two polylines given by the node lists {@code n1}
     * and {@code n2} are "direction aligned". Their direction is aligned,
     * if the two lines between the two start nodes and the two end nodes
     * of {@code n1} and code {@code n2} respectively, do not intersect.</p>
     *
     * @param n1 the first list of nodes
     * @param n2 the second list of nodes
     * @return true, if the two polylines are "direction aligned".
     */
    protected boolean areDirectionAligned(List<Node> n1, List<Node> n2) {
        /*
         * Check whether n1 and n2 start and end at the same nodes
         */
        if (haveSameStartAndEndNode(n1, n2)) return true;
        if (haveReversedStartAndEndeNode(n1,n2)) return false;

        /**
         * new heuristic. The endpoints of will be merged with the end points
         * of t. We compute the total distance between the endpoints of s
         * and t and the endpoints of s and t in reverse direction. Choose
         * the direction with the minimal total distance the endpoints have
         * to be moved.
         *
         */
        final EastNorth s1 = n1.get(0).getEastNorth();
        final EastNorth s2 = n1.get(n1.size()-1).getEastNorth();

        final EastNorth t1 = n2.get(0).getEastNorth();
        final EastNorth t2 = n2.get(n2.size()-1).getEastNorth();

        double d1 = Math.abs(s1.distance(t1)) + Math.abs(s2.distance(t2));
        double d2 = Math.abs(s1.distance(t2)) + Math.abs(s2.distance(t1));
        return d1 <= d2;
    }

    /**
     * <p>Replies true, if the two way slices are "direction aligned".</p>
     *
     * @param dragSource the first way slice
     * @param dropTarget the second way slice
     * @return  true, if the two way slices are "direction aligned"
     * @see #areDirectionAligned(List, List)
     */
    protected boolean areDirectionAligned(WaySlice dragSource,
            WaySlice dropTarget){
        if (dragSource == null) return false;
        if (dropTarget == null) return false;

        return areDirectionAligned(dragSource.getNodes(),
                dropTarget.getNodes());
    }

    protected void ensureSelectedNodesConsistent() {
        Iterator<Node> it = selectedNodes.iterator();
        while(it.hasNext()) {
            Node n = it.next();
            if (!layer.data.getNodes().contains(n)) {
                it.remove();
            } else if (getFilteredSet(n.getReferrers(),
                    Way.class).isEmpty()) {
                it.remove();
            } else if (n.isDeleted()) {
                it.remove();
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* interface DataSetListener                                             */
    /* --------------------------------------------------------------------- */

    @Override
    public void primitivesAdded(PrimitivesAddedEvent arg0) {/* ignore */}

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        ensureSelectedNodesConsistent();
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        ensureSelectedNodesConsistent();
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        ensureSelectedNodesConsistent();
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event)
    {/* ignore */}
    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event)
    {/* ignore */}
    @Override
    public void tagsChanged(TagsChangedEvent event) { /* ignore */}
    @Override
    public void nodeMoved(NodeMovedEvent event) {/* ignore */}
}
