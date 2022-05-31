package org.openstreetmap.josm.plugins.contourmerge

import groovy.test.GroovyTestCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.osm.WaySegment
import org.openstreetmap.josm.gui.layer.OsmDataLayer
import org.openstreetmap.josm.plugins.contourmerge.fixture.JOSMFixture

class ContourMergeModelTest extends GroovyTestCase {

    protected DataSet ds

    def newNode(int id){
        Node n = new Node(id)
        ds.addPrimitive(n)
        return n
    }

    def newWay(int id, Node... nodes){
        return newWay(id,Arrays.asList(nodes))
    }

    def newWay(int id, List<Node> nodes){
        Way w = new Way(id,1)
        w.setNodes(nodes)
        ds.addPrimitive(w)
        return w
    }

    @BeforeAll
    static void setupJosmFixture() {
        final fixture = JOSMFixture.createFixture()
    }

    @BeforeEach
    void setUp() {
        ds = new DataSet()
    }

    def createModelMock(){
        OsmDataLayer layer = new OsmDataLayer(ds, null, null)
        ContourMergeModel model = new ContourMergeModel(layer)
        return model
    }

    @Test
    void selectNode() {
        ContourMergeModel model = createModelMock()
        Node n = newNode(1)
        model.selectNode(n)
        assertTrue(model.isSelected(n))
        model.deselectNode(n)
        assertTrue(!model.isSelected(n))
    }

    @Test
    void selectNodeAndDeselectAll() {
        ContourMergeModel model = createModelMock()
        model.selectNode(newNode(1))
        model.selectNode(newNode(2))
        assertEquals(2, model.getSelectedNodes().size())
        model.deselectAllNodes()
        assertTrue(model.getSelectedNodes().isEmpty())
    }

    @Test
    void toggleSelected(){
        ContourMergeModel model = createModelMock()
        Node n = newNode(1)
        model.selectNode(n)
        model.toggleSelected(n)
        assert !model.isSelected(n)
        model.toggleSelected(n)
        assert model.isSelected(n)
    }

    @Test
    void getDragSource_OpenWayNoSelectedNodes() {
        Node n1 = newNode(1)
        Node n2 = newNode(2)
        Node n3 = newNode(3)
        Node n4 = newNode(4)
        Way w = newWay(1,n1,n2,n3,n4)

        WaySegment ws = new WaySegment(w, 1)
        ContourMergeModel model = createModelMock()
        model.setDragStartFeedbackWaySegment(ws)
        WaySlice slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == w.nodesCount-1

        ws = new WaySegment(w, 0)
        model = createModelMock()
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == w.nodesCount-1

        ws = new WaySegment(w, 2)
        model = createModelMock()
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == w.nodesCount-1
    }

    @Test
    void getDragSource_OpenWayOneSelectedNode() {
        Node n1 = newNode(1)
        Node n2 = newNode(2)
        Node n3 = newNode(3)
        Node n4 = newNode(4)
        Way w = newWay(1,n1,n2,n3,n4)
        WaySegment ws
        ContourMergeModel model
        WaySlice slice

        /*
         * n1----------n2---------n3-----------n4
         *             x                              ~selected
         *       ^                                    ~drag start
         */
        model = createModelMock()
        model.selectNode(n2)
        ws = new WaySegment(w, 0)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == 1

        /*
        * n1----------n2---------n3-----------n4
        *             x                            ~selected
        *                 ^                        ~drag start
        */
        model = createModelMock()
        model.selectNode(n2)
        ws = new WaySegment(w, 1)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 1
        assert slice.end == 3

        /*
        * n1----------n2---------n3-----------n4
        *             x                           ~selected
        *                                ^        ~drag start
        */
        model = createModelMock()
        model.selectNode(n2)
        ws = new WaySegment(w, 2)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 1
        assert slice.end == 3
    }

    @Test
    void getDragSource_OpenWayFirstOrLastSelectedNode() {
        Node n1 = newNode(1)
        Node n2 = newNode(2)
        Node n3 = newNode(3)
        Node n4 = newNode(4)
        Way w = newWay(1,n1,n2,n3,n4)
        WaySegment ws
        ContourMergeModel model
        WaySlice slice

        /*
         * n1----------n2---------n3-----------n4
         * x                                          ~selected
         *                     ^                      ~drag start
         */
        model = createModelMock()
        model.selectNode(n1)
        ws = new WaySegment(w, 1)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == 3

        /*
        * n1----------n2---------n3-----------n4
        *                                      x   ~selected
        *                 ^                        ~drag start
        */
        model = createModelMock()
        model.selectNode(n4)
        ws = new WaySegment(w, 1)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == 3
    }

    @Test
    void getDragSource_ClosedWayTwoSelectedNodes() {
        Node n1 = newNode(1)
        Node n2 = newNode(2)
        Node n3 = newNode(3)
        Node n4 = newNode(4)
        Node n5 = newNode(5)
        Way w = newWay(1,n1,n2,n3,n4,n5,n1)
        WaySegment ws
        ContourMergeModel model
        WaySlice slice

        /*
         *+------------------------------------------------+
        * |                                                |
        * n1----------n2---------n3-----------n4-----------n5
        *             x                       x                ~selected
        *                    ^                                 ~drag start
        */
        model = createModelMock()
        model.selectNode(n2)
        model.selectNode(n4)
        ws = new WaySegment(w, 1)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 1
        assert slice.end == 3

        /*
        *+------------------------------------------------+
       * |                                                |
       * n1----------n2---------n3-----------n4-----------n5
       *             x                       x                ~selected
       *                             ^                        ~drag start
       */
       model = createModelMock()
       model.selectNode(n2)
       model.selectNode(n4)
       ws = new WaySegment(w, 1)
       model.setDragStartFeedbackWaySegment(ws)
       slice = model.getDragSource()
       assert slice != null
       assert slice.way == w
       assert slice.start == 1
       assert slice.end == 3

       /*
       *+------------------------------------------------+
      * |                                                |
      * n1----------n2---------n3-----------n4-----------n5
      *             x                       x                ~selected
      *                                          ^           ~drag start
      */
      model = createModelMock()
      model.selectNode(n2)
      model.selectNode(n4)
      ws = new WaySegment(w, 3)
      model.setDragStartFeedbackWaySegment(ws)
      slice = model.getDragSource()
      assert slice != null
      assert slice.way == w
      assert slice.start == 1
      assert slice.end == 3
      assert !slice.inDirection

      /*
      *+------------------------------------------------+
     * |                                                |
     * n1----------n2---------n3-----------n4-----------n5
     *             x                       x                ~selected
     *      ^                                               ~drag start
     */
     model = createModelMock()
     model.selectNode(n2)
     model.selectNode(n4)
     ws = new WaySegment(w, 0)
     model.setDragStartFeedbackWaySegment(ws)
     slice = model.getDragSource()
     assert slice != null
     assert slice.way == w
     assert slice.start == 1
     assert slice.end == 3
     assert !slice.inDirection

         /*
         *+------------------------------------------------+
        * |                                                |
        * n1----------n2---------n3-----------n4-----------n5
        * x                      x                            ~selected
        *        ^                                            ~drag start
        */
        model = createModelMock()
        model.selectNode(n1)
        model.selectNode(n3)
        ws = new WaySegment(w, 0)
        model.setDragStartFeedbackWaySegment(ws)
        slice = model.getDragSource()
        assert slice != null
        assert slice.way == w
        assert slice.start == 0
        assert slice.end == 2
        assert slice.inDirection

       /*
       * +------------------------------------------------+
       * |                                                |
       * n1----------n2---------n3-----------n4-----------n5
       * x                      x                            ~selected
       *                                   ^                 ~drag start
       */
       model = createModelMock()
       model.selectNode(n1)
       model.selectNode(n3)
       ws = new WaySegment(w, 2)
       model.setDragStartFeedbackWaySegment(ws)
       slice = model.getDragSource()
       assert slice != null
       assert slice.way == w
       assert slice.start == 0
       assert slice.end == 2
       assert !slice.inDirection

       /*
        *             v ~drag start
       * +------------------------------------------------+
       * |                                                |
       * n1----------n2---------n3-----------n4-----------n5
       * x                                                x   ~selected
       */
       model = createModelMock()
       model.selectNode(n1)
       model.selectNode(n5)
       ws = new WaySegment(w, 4)
       model.setDragStartFeedbackWaySegment(ws)
       slice = model.getDragSource()
       assert slice != null
       assert slice.way == w
       assert slice.start == 0
       assert slice.end == 4
       assert !slice.inDirection
    }

    @Test
    void isWaySegmentDragable() {
        Node n1 = newNode(1)
        Node n2 = newNode(2)
        Node n3 = newNode(3)
        Node n4 = newNode(4)
        Node n5 = newNode(5)
        ContourMergeModel model

        // an open way
        Way w = newWay(1,n1,n2,n3,n4)

        /*
         * n1----------n2---------n3-----------n4
         *                                            ~selected
         */
        model = createModelMock()
        (0..2).each {
            // we can start a drag on each segment, even if no nodes are selected
            assert model.isWaySegmentDragable(new WaySegment(w, it))
        }

        /*
        * n1----------n2---------n3-----------n4
        *             x                       x        ~selected
        */
       model = createModelMock()
       (0..2).each {
           // we can start a drag on each segment, regardless of the number
           // of selected nodes
           assert model.isWaySegmentDragable(new WaySegment(w, it))
       }

        // a closed way
        w = newWay(2,n1,n2,n3,n4,n5,n1)
        /*
         *+------------------------------------------------+
        * |                                                |
        * n1----------n2---------n3-----------n4-----------n5
        *             x                       x                ~selected
        */
        model = createModelMock()
        model.selectNode(n2)
        model.selectNode(n4)
        (0..4).each {
            // we can start a drag operation on each segment
            assert model.isWaySegmentDragable(new WaySegment(w, it))
        }

        /*
        *+------------------------------------------------+
       * |                                                |
       * n1----------n2---------n3-----------n4-----------n5
       *             x                                        ~selected
       */
       model = createModelMock()
       model.selectNode(n2)
       (0..4).each {
           // we can start a drag operation on any segment, because there
           // is only one node selected
           assert ! model.isWaySegmentDragable(new WaySegment(w, it))
       }

       /*
       *+------------------------------------------------+
      * |                                                |
      * n1----------n2---------n3-----------n4-----------n5
      *                                                      ~selected
      */
      model = createModelMock()
      (0..4).each {
          // we can start a drag operation on any segment, because there
          // is no node selected
          assert ! model.isWaySegmentDragable(new WaySegment(w, it))
      }
    }
 }
