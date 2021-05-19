package org.openstreetmap.josm.plugins.contourmerge;

import org.apache.commons.lang3.Validate;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;

import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Optional;

/**
 * <strong>ContourMergeView</strong> renders the {@link ContourMergeModel}
 * for the currently active data layer.
 */
public class ContourMergeView implements MapViewPaintable{

    static private ContourMergeView instance;

    public static ContourMergeView getInstance() {
        return instance == null ? instance = new ContourMergeView() : instance;
    }

    public void wireToJOSM() {
        final MapView mv = MainApplication.getMap().mapView; 
        if (mv != null) {
            mv.addTemporaryLayer(this);
        }
    }

    public void unwireFromJOSM() {
        Optional.ofNullable(MainApplication.getMap())
        .map(frame -> frame.mapView)
        .ifPresent(mv -> mv.removeTemporaryLayer(ContourMergeView.this));
    }

    protected Optional<ContourMergeModel> getActiveModel() {
        return ContourMergePlugin.getModelManager().getActiveModel();
    }

    protected void decorateFeedbackNode(Graphics2D g, MapView mv, Bounds bbox){
        /* currently no decoration - mouse pointer is changing if mouse over a
         * node */
    }

    protected void decorateSelectedNode(Graphics2D g, MapView mv, Bounds bbox,
            Node node){
        // since at least 12712 the following check always returns false. Possibly
        // already before 12712. Don't know due to which commit exactly.
        // Comment it out for the time being. Should have only a minor
        // impact on drawing efficiency and responsiveness.
        //if (!bbox.contains(node.getCoor())) return;
        Point p = mv.getPoint(node.getCoor());
        g.translate(p.x,p.y);
            g.setColor(Color.ORANGE);
            g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.drawLine(-5, 5, 5,-5);
            g.drawLine(-5, -5, 5, 5);
            g.setColor(Color.ORANGE.brighter());
            g.setStroke(new BasicStroke(1,BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.drawLine(-5, 5, 5,-5);
            g.drawLine(-5, -5, 5, 5);
        g.translate(-p.x, -p.y);
    }

    protected void decorateSelectedNodes(Graphics2D g, MapView mv, Bounds bbox){
        getActiveModel().ifPresent(model -> {
            model.getSelectedNodes().stream()
            .forEach(n -> decorateSelectedNode(g, mv, bbox, n));
        });
    }

    /**
     * <p>Highlights a way slice, i.e. the current drag source or a potential
     * drop target.</p>
     *
     * @param g graphics context
     * @param mv map view
     * @param bbox map bbox
     * @param slice the way slice. Must not be null.
     */
    protected void highlightWaySlice(Graphics2D g, MapView mv, Bounds bbox,
            WaySlice slice){
        Path2D polyline = project(mv, slice);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND));
        g.draw(polyline);
    }

    /**
     * <p>Projects this way slice onto the map view {@code mv}. Replies a
     * polyline representing the way slice on screen.</p>
     *
     * @param mv the map view. Must not be null.
     * @param ws the way slice. Must not be null.
     * @return a polyline
     *
     */
    public static Path2D project(@NotNull MapView mv, @NotNull WaySlice ws){
        Validate.notNull(mv);
        Validate.notNull(ws);
        Path2D.Float polyline = new Path2D.Float();
        if (ws.isInDirection()) {
            /*
             * a way slice of an open way, or a closed way between two nodes in
             * the ways direction
             */
            for (int i = ws.getStart(); i <= ws.getEnd(); i++){
                Point p = mv.getPoint(ws.getWay().getNode(i).getCoor());
                if (i == ws.getStart()) {
                    polyline.moveTo(p.x, p.y);
                } else {
                    polyline.lineTo(p.x, p.y);
                }
            }
        } else {
            /*
             * a way slice of a closed way, between two nodes in the
             * direction *opposite* to the ways direction
             */
            for (int i = ws.getStart(); i >= 0; i--){
                Point p = mv.getPoint(ws.getWay().getNode(i).getCoor());
                if (i == ws.getStart()) {
                    polyline.moveTo(p.x, p.y);
                } else {
                    polyline.lineTo(p.x, p.y);
                }
            }
            for (int i = ws.getWay().getNodesCount()-2; i >= ws.getEnd(); i--){
                Point p = mv.getPoint(ws.getWay().getNode(i).getCoor());
                if (i == ws.getStart()) {
                    polyline.moveTo(p.x, p.y);
                } else {
                    polyline.lineTo(p.x, p.y);
                }
            }
        }
        return polyline;
    }

    /**
     * <p>Projects this way slice onto the map view {@code mv}. Replies a
     * polyline representing the way slice on screen, displaced by the offset
     * {@code displacement.x} in x-direction and {@code displacement.y}
     * in y-direction. </p>
     *
     * @param mv the map view. Must not be null.
     * @param ws the way slice. Must not be null.
     * @param displacement the displacement. (0,0) is assumed, if null.
     * @return a polyline
     */
    public Path2D project(@NotNull MapView mv, @NotNull WaySlice ws,
            Point displacement){
        Validate.notNull(mv);
        Validate.notNull(ws);
        if (displacement == null) displacement = new Point(0,0);
        Path2D polyline = project(mv, ws);
        AffineTransform at = new AffineTransform();
        at.setToTranslation(displacement.x, displacement.y);
        polyline = new Path2D.Float(polyline, at);
        return polyline;
    }

    protected void paintHelperLinesFromDragSourceToDraggedWaySlice(
            Graphics2D g, MapView mv){
        getActiveModel()
        .filter(ContourMergeModel::isDragging)
        .ifPresent(model -> {
            WaySlice dragSource = model.getDragSource();

            Node lowerTearOffNode = dragSource.getStartTearOffNode();
            Node upperTearOffNode = dragSource.getEndTearOffNode();
            Point offset = model.getDragOffset();

            // init the graphics attributes
            float[] dashPattern = { 2, 3, 2, 3 };
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                       BasicStroke.JOIN_ROUND,1f, dashPattern,0f));
            boolean crossing = helperLinesAreCrossing(mv, dragSource, offset);
            if (lowerTearOffNode != null){
                Point p1 = mv.getPoint(!crossing
                        ? dragSource.getStartNode()
                        : dragSource.getEndNode());
                p1 = new Point(p1.x + offset.x, p1.y + offset.y);
                Point p2 = mv.getPoint(lowerTearOffNode);
                g.drawLine(p1.x,p1.y, p2.x,p2.y);
            }
            if (upperTearOffNode != null){
                Point p1 = mv.getPoint(!crossing
                        ? dragSource.getEndNode()
                        : dragSource.getStartNode());
                p1 = new Point(p1.x + offset.x, p1.y + offset.y);
                Point p2 = mv.getPoint(upperTearOffNode);
                g.drawLine(p1.x,p1.y, p2.x,p2.y);
            }
        });
    }

    /**
     * <p>Checks whether the two helper lines from the drag source to the
     * drop target intersect, because the ways of the drag source and the
     * drop target don't have the same direction.</p>
     *
     * <p>If the helpers intersect, we will reverse the two end points of
     * the drop target, when we paint the helper lines.</p>
     *
     * @param mv the map view
     * @param dragSource the drag source
     * @param dropTarget the drop target
     * @return true, if the two helper lines from the drag source to the
     * drop target intersect
     */
    protected boolean helperLinesAreCrossing(MapView mv, WaySlice dragSource,
            WaySlice dropTarget){
        Node s1 = dragSource.getStartTearOffNode();
        if (s1 == null) s1 = dragSource.getStartNode();
        Node s2= dragSource.getEndTearOffNode();
        if (s2 == null) s2 = dragSource.getEndNode();
        Point sp1 = mv.getPoint(s1);
        Point sp2 = mv.getPoint(s2);

        Point tp1 = mv.getPoint(dropTarget.getStartNode());
        Point tp2 = mv.getPoint(dropTarget.getEndNode());
        return helperLinesAreCrossing(sp1,sp2,tp1,tp2);
    }

    protected boolean helperLinesAreCrossing(Point s1, Point s2, Point t1,
            Point t2){
        Line2D l1 = new Line2D.Float(s1.x, s1.y, t1.x,t1.y);
        Line2D l2 = new Line2D.Float(s2.x, s2.y, t2.x,t2.y);
        return l1.intersectsLine(l2);
    }

    /**
     * <p>Checks whether the two helper lines from the drag source to the
     * currently painted drag object offset by {@code dragOffset}
     * intersect.</p>
     *
     * <p>If the helpers intersect, we will reverse the two end points of the
     * drop target, when we paint the helper lines.</p>
     *
     * @param mv the map view
     * @param dragSource the drag source
     * @param dragOffset the drag offset
     * @return true, if the two helper lines from the drag source to the drop
     * target intersect
     */
    protected boolean helperLinesAreCrossing(MapView mv, WaySlice dragSource,
            Point dragOffset){
        Node s1 = dragSource.getStartTearOffNode();
        if (s1 == null) s1 = dragSource.getStartNode();
        Node s2= dragSource.getEndTearOffNode();
        if (s2 == null) s2 = dragSource.getEndNode();
        Point sp1 = mv.getPoint(s1);
        Point sp2 = mv.getPoint(s2);

        Point tp1 = mv.getPoint(dragSource.getStartNode());
        tp1 = new Point(tp1.x + dragOffset.x, tp1.y + dragOffset.y);

        Point tp2 = mv.getPoint(dragSource.getEndNode());
        tp2 = new Point(tp2.x + dragOffset.x, tp2.y + dragOffset.y);

        return helperLinesAreCrossing(sp1,sp2,tp1,tp2);
    }

    protected void paintHelperLinesFromDragSourceToDropTarget(Graphics2D g,
            MapView mv){
        getActiveModel()
        .filter(ContourMergeModel::isDragging)
        .ifPresent(model -> {
            WaySlice dragSource = model.getDragSource();
            WaySlice dropTarget = model.getDropTarget();
            Node lowerTearOffNode = dragSource.getStartTearOffNode();
            Node upperTearOffNode = dragSource.getEndTearOffNode();

            // init the graphics attributes
            float[] dashPattern = { 2, 3, 2, 3 };
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND,1f, dashPattern,0f));

            boolean crossing = helperLinesAreCrossing(mv, dragSource, dropTarget);
            if (lowerTearOffNode != null){
                Point p1 = mv.getPoint(lowerTearOffNode);
                Point p2 = mv.getPoint(!crossing ? dropTarget.getStartNode()
                        : dropTarget.getEndNode());
                g.drawLine(p1.x,p1.y, p2.x,p2.y);
            }
            if (upperTearOffNode != null){
                Point p1 = mv.getPoint(upperTearOffNode);
                Point p2 = mv.getPoint(!crossing ? dropTarget.getEndNode()
                        : dropTarget.getStartNode());
                g.drawLine(p1.x,p1.y, p2.x,p2.y);
            }
        });
    }

    protected void paintDraggedWaySlice(Graphics2D g, MapView mv, Bounds bbox) {
        getActiveModel()
        .filter(ContourMergeModel::isDragging)
        .ifPresent(model -> {
            WaySlice dragSource = model.getDragSource();
            WaySlice dropTarget = model.getDropTarget();
            if (dragSource == null) return;
            if (dropTarget == null) {
                /*
                 * paint the temporary dragged way slice, unless the mouse is
                 * currently over a potential drop target
                 */
                Path2D polyline = project(mv, dragSource,
                        model.getDragOffset());
                g.setColor(Color.RED);
                float[] dashPattern = { 10, 5, 10, 5 };
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND,1f, dashPattern,0f));
                g.draw(polyline);
                paintHelperLinesFromDragSourceToDraggedWaySlice(g, mv);
            } else {
                /*
                 * the mouse is over a suitable drop target. Paint only
                 * two helper lines from the drag source to the drop target.
                 * The drop target is highlighted elsewhere.
                 */
                paintHelperLinesFromDragSourceToDropTarget(g,mv);
            }
        });
    }

    /* ---------------------------------------------------------------------- */
    /* interface MapViewPaintable                                             */
    /* ---------------------------------------------------------------------- */
    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (!ContourMergePlugin.isEnabled()) return;
        getActiveModel()
            .filter(model -> model.getLayer().isVisible())
            .ifPresent(model -> {
                decorateSelectedNodes(g, mv, bbox);
                decorateFeedbackNode(g, mv, bbox);
                WaySlice dragSourceSlice = model.getDragSource();
                if (dragSourceSlice != null){
                    highlightWaySlice(g, mv, bbox, dragSourceSlice);
                }
                WaySlice dropTargetSlice = model.getDropTarget();
                if (dropTargetSlice != null){
                    highlightWaySlice(g, mv, bbox, dropTargetSlice);
                }
                if (model.isDragging()){
                    paintDraggedWaySlice(g, mv, bbox);
                }
            });
    }
}
