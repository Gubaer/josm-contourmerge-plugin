package org.openstreetmap.josm.plugins.contourmerge

import groovy.test.GroovyTestCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.data.Preferences
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.projection.ProjectionRegistry
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.gui.layer.OsmDataLayer
import org.openstreetmap.josm.io.OsmReader
import org.openstreetmap.josm.spi.preferences.Config

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertFalse

class DataIntegrityProblemTest01 extends GroovyTestCase {

    DataSet dataSet
    OsmDataLayer layer
    ContourMergeModel mergeModel

    Way closedWay
    Way openWay

    WaySlice closedWaySlice
    List<Node> closedWayNodes
    WaySlice openWaySlice

    @BeforeAll
    static void initJosmConfig() {
        Config.setPreferencesInstance(new Preferences())
        ProjectionRegistry.setProjection(
                Projections.getProjectionByCode("EPSG:3857"))
    }

    static def buildNode(Map args) {
        def node = new Node(args.id as long)
        node.setCoor(new LatLon(args.lat as double, args.lon as double))
        return node
    }

    def createDataSet() {
        // create a dataset
        // add two ways
        // - a closed way with four nodes
        // - an open node with 6 nodes, which shares two nodes with
        //  the other way
        dataSet = new DataSet()

        //def joinNode = new Node(1, new LatLon(0,0))
        def joinNode = buildNode(id: 0, lat: 0, lon: 0)
        closedWayNodes = [
            joinNode,
            buildNode(id: 1, lat: 0, lon: -1),
            buildNode(id: 2, lat: -1, lon: -1),
            buildNode(id: 3, lat: 0, lon: -1),
            joinNode
        ]

        closedWayNodes[0..3].each {node ->
            dataSet.addPrimitive(node)
        }
        closedWay = new Way()
        closedWay.setNodes(closedWayNodes)
        dataSet.addPrimitive(closedWay)

        def openWayNodes = [
            buildNode(id: 10, lat: 1, lon: -3),
            buildNode(id: 11, lat: 1, lon: -2),
            buildNode(id: 12, lat: 1, lon: -1),
            buildNode(id: 13, lat: 1, lon: 0),
            buildNode(id: 14, lat: 1, lon: 1),
            buildNode(id: 15, lat: 1, lon: 2),
        ]
        openWayNodes.each {node ->
            dataSet.addPrimitive(node)
        }
        openWay = new Way()
        openWay.setNodes(openWayNodes)
        dataSet.addPrimitive(openWay)
    }

    def prepareMergeModel() {
        layer = new OsmDataLayer(dataSet, "test name", null /* no file */)
        mergeModel = new ContourMergeModel(layer)
    }

    @BeforeEach
    void prepareTestData() {
        createDataSet()
        prepareMergeModel()
    }

    @Test
    void "test case 01"() {
        closedWaySlice = new WaySlice(closedWay, 3, 4, true /* in direction */)
        openWaySlice = new WaySlice(openWay, 2, 3, true /* in direction */)

        def source = closedWaySlice
        def target = openWaySlice
        def mergeCommand = mergeModel.buildContourAlignCommand(
            source,
            target
        )
        mergeCommand.executeCommand()

        assertTrue("closedWay should still be closed", closedWay.isClosed())
        assertTrue("openWay should still be open", !openWay.isClosed())
        assertTrue("the original join node of the closed way should be deleted",
            closedWayNodes[0].isDeleted())

        openWay.getNodes().eachWithIndex{ node, i ->
            assertFalse("the ${i}-th node of the open way should not be deleted",
            openWay.getNode(i).isDeleted())
        }
        def openWayNodeIds = openWay.getNodes().collect {Node node ->
            node.getId()}
        assertThat(openWayNodeIds, equalTo([10l, 11l, 12l, 13l, 14l, 15l]))

        def closedWayIds = closedWay.getNodes().collect {Node node ->
            node.getId()}
        assertThat(closedWayIds, equalTo([1l, 2l, 12l, 13l, 1l], ))

        closedWay.getNodes().eachWithIndex{ node, i ->
            assertFalse("the ${i}-th node of the closed way should not be deleted",
                closedWay.getNode(i).isDeleted())
        }

        def numDeletedNodes = dataSet.getNodes().grep {
            node -> node.isDeleted()
        }.size()

        assertEquals("should have 2 deleted nodes, got ${numDeletedNodes}",
            2, numDeletedNodes, )
    }
}


class DataIntegrityProblemTest02 extends GroovyTestCase {

    DataSet dataSet
    OsmDataLayer layer
    ContourMergeModel mergeModel

    Way closedWay
    Way openWay

    WaySlice closedWaySlice
    List<Node> closedWayNodes
    WaySlice openWaySlice

    @BeforeAll
    static void initJosmConfig() {
        Config.setPreferencesInstance(new Preferences())
        ProjectionRegistry.setProjection(
                Projections.getProjectionByCode("EPSG:3857"))
    }

    static def buildNode(Map args) {
        def node = new Node(args.id as long)
        node.setCoor(new LatLon(args.lat as double, args.lon as double))
        return node
    }

    def createDataSet() {
        // create a dataset
        // add two ways
        // - a closed way with four nodes
        // - an open node with 6 nodes, which shares two nodes with
        //  the other way
        dataSet = new DataSet()

        //def joinNode = new Node(1, new LatLon(0,0))
        def joinNode = buildNode(id: 0, lat: 0, lon: 0)
        closedWayNodes = [
                joinNode,
                buildNode(id: 1, lat: 1, lon: -1),
                buildNode(id: 2, lat: 1, lon: -2),
                buildNode(id: 3, lat: 0, lon: -3),
                buildNode(id: 4, lat: -1, lon: -2),
                buildNode(id: 5, lat: -1, lon: -1),
                joinNode
        ]

        closedWayNodes[0..5].each {node ->
            dataSet.addPrimitive(node)
        }
        closedWay = new Way()
        closedWay.setNodes(closedWayNodes)
        dataSet.addPrimitive(closedWay)

        def openWayNodes = [
                buildNode(id: 10, lat: 1, lon: -3),
                buildNode(id: 11, lat: 1, lon: -2),
                buildNode(id: 12, lat: 1, lon: -1),
                buildNode(id: 13, lat: 1, lon: 0),
                buildNode(id: 14, lat: 1, lon: 1),
                buildNode(id: 15, lat: 1, lon: 2),
        ]
        openWayNodes.each {node ->
            dataSet.addPrimitive(node)
        }
        openWay = new Way()
        openWay.setNodes(openWayNodes)
        dataSet.addPrimitive(openWay)
    }

    def prepareMergeModel() {
        layer = new OsmDataLayer(dataSet, "test name", null /* no file */)
        mergeModel = new ContourMergeModel(layer)
    }


    @BeforeEach
    void prepareTestData() {
        createDataSet()
        prepareMergeModel()
    }

    @Test
    void "test case 02"() {
        closedWaySlice = new WaySlice(closedWay, 1, 5, false /* in direction */)
        openWaySlice = new WaySlice(openWay, 2, 3, true /* in direction */)

        def source = closedWaySlice
        def target = openWaySlice
        def mergeCommand = mergeModel.buildContourAlignCommand(
                source,
                target
        )
        mergeCommand.executeCommand()

        assertTrue("closedWay should still be closed", closedWay.isClosed())
        assertTrue("openWay should still be open", !openWay.isClosed())
        assertTrue("the original join node of the closed way should be deleted",
                closedWayNodes[0].isDeleted())

        openWay.getNodes().eachWithIndex{ node, i ->
            assertFalse("the ${i}-th node of the open way should not be deleted",
                    openWay.getNode(i).isDeleted())
        }
        def openWayNodeIds = openWay.getNodes().collect {Node node ->
            node.getId()}
        println("openWayNodeIds: ${openWayNodeIds}")
        //assertThat(openWayNodeIds, equalTo([10l, 11l, 12l, 13l, 14l, 15l]))

        def closedWayIds = closedWay.getNodes().collect {Node node ->
            node.getId()}
        //assertThat(closedWayIds, equalTo([1l, 2l, 12l, 13l, 1l], ))
        println("closedWayIds: ${closedWayIds}")

        closedWay.getNodes().eachWithIndex{ node, i ->
            assertFalse("the ${i}-th node of the closed way should not be deleted",
                    closedWay.getNode(i).isDeleted())
        }

        def numDeletedNodes = dataSet.getNodes().grep {
            node -> node.isDeleted()
        }.size()

        assertEquals("should have 3 deleted nodes, got ${numDeletedNodes}",
                3, numDeletedNodes, )
    }
}

class DataIntegrityProblemTest03 extends GroovyTestCase {

    // dataset reported in ticket https://josm.openstreetmap.de/ticket/20629
    final static DATA_SET = """
<osm version='0.6' generator='JOSM'>
  <bounds minlat='-23.1550787' minlon='47.0651293' maxlat='-23.1489626' maxlon='47.0745707' origin='CGImap 0.8.3 (2888343 spike-08.openstreetmap.org)' />
  <bounds minlat='-23.1550787' minlon='47.0651293' maxlat='-23.1489626' maxlon='47.0745707' origin='OpenStreetMap server' />
  <node id='-102544' action='modify' visible='true' lat='-23.15020552151' lon='47.0683025658' />
  <node id='-102545' action='modify' visible='true' lat='-23.15045423564' lon='47.06711683267' />
  <node id='-102546' action='modify' visible='true' lat='-23.1513673192' lon='47.06758000968' />
  <node id='-102547' action='modify' visible='true' lat='-23.15138776129' lon='47.06829144955' />
  <node id='-102548' action='modify' visible='true' lat='-23.15059733124' lon='47.06959946141' />
  <node id='-102549' action='modify' visible='true' lat='-23.15108453652' lon='47.06960687224' />
  <node id='-102550' action='modify' visible='true' lat='-23.15100385464' lon='47.06707902709' />
  <node id='-102551' action='modify' visible='true' lat='-23.15019717563' lon='47.06768486771' />
  <node id='-102552' action='modify' visible='true' lat='-23.15020538297' lon='47.06919191663' />
  <node id='-102553' action='modify' visible='true' lat='-23.15139691206' lon='47.06905852456' />
  <way id='-104797' action='modify' visible='true'>
    <nd ref='-102544' />
    <nd ref='-102551' />
    <nd ref='-102545' />
    <nd ref='-102550' />
    <nd ref='-102546' />
    <nd ref='-102547' />
    <nd ref='-102544' />
    <tag k='natural' v='water' />
    <tag k='water' v='pond' />
  </way>
  <way id='-104798' action='modify' visible='true'>
    <nd ref='-102547' />
    <nd ref='-102544' />
    <nd ref='-102552' />
    <nd ref='-102548' />
    <nd ref='-102549' />
    <nd ref='-102553' />
    <nd ref='-102547' />
    <tag k='natural' v='wood' />
  </way>
</osm>
"""

    @BeforeAll
    static void initJosmConfig() {
        Config.setPreferencesInstance(new Preferences())
        ProjectionRegistry.setProjection(
                Projections.getProjectionByCode("EPSG:3857"))
    }

    DataSet dataSet
    def layer
    def mergeModel

    def createDataSet() {
        dataSet = OsmReader.parseDataSet(
            new ByteArrayInputStream(DATA_SET.getBytes()),
            null // no progress monitor
        )
    }

    def prepareMergeModel() {
        layer = new OsmDataLayer(dataSet, "test 03", null /* no file */)
        mergeModel = new ContourMergeModel(layer)
    }


    @BeforeEach
    void prepareTestData() {
        createDataSet()
        prepareMergeModel()
    }

    @Test
    void "test case 03"() {

        final pond = dataSet.getPrimitiveById(-104797, OsmPrimitiveType.WAY) as Way
        final wood = dataSet.getPrimitiveById(-104798, OsmPrimitiveType.WAY) as Way

        def sourceSlice = new WaySlice(wood, 0, 1, true /* in direction */)
        def targetSlice = new WaySlice(pond, 5, 6, true /* in direction */)

        def mergeCommand = mergeModel.buildContourAlignCommand(
                sourceSlice,
                targetSlice
        )
        mergeCommand.executeCommand()

        // https://josm.openstreetmap.de/ticket/20629 reports that both
        // the pond and the wood way refer to deleted nodes after the merge
        // operation.
        //
        // Make sure there are no deleted nodes after the merge operation.
        // As a consequence, neither the wood nor the pond way still refer
        // to deleted nodes.
        def numDeletedNodes = dataSet.getPrimitives {obj ->
            obj.isDeleted() && obj.getType() == OsmPrimitiveType.NODE
        }.size()
        assertTrue("has $numDeletedNodes nodes, expected none", numDeletedNodes == 0)
    }
}



