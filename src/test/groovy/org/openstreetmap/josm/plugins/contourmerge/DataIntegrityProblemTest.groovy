package org.openstreetmap.josm.plugins.contourmerge

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.openstreetmap.josm.data.Preferences
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.projection.ProjectionRegistry
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.gui.layer.OsmDataLayer
import org.openstreetmap.josm.spi.preferences.Config

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.*

class DataIntegrityProblemTest01 {

    DataSet dataSet
    OsmDataLayer layer
    ContourMergeModel mergeModel

    Way closedWay
    Way openWay

    WaySlice closedWaySlice
    List<Node> closedWayNodes
    WaySlice openWaySlice

    @BeforeClass
    static void initJosmConfig() {
        Config.setPreferencesInstance(new Preferences())
        ProjectionRegistry.setProjection(
                Projections.getProjectionByCode("EPSG:3857"))
    }

    def buildNode(Map args) {
        def node = new Node(args.id)
        node.setCoor(new LatLon(args.lat, args.lon))
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


    @Before
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


class DataIntegrityProblemTest02 {

    DataSet dataSet
    OsmDataLayer layer
    ContourMergeModel mergeModel

    Way closedWay
    Way openWay

    WaySlice closedWaySlice
    List<Node> closedWayNodes
    WaySlice openWaySlice

    @BeforeClass
    static void initJosmConfig() {
        Config.setPreferencesInstance(new Preferences())
        ProjectionRegistry.setProjection(
                Projections.getProjectionByCode("EPSG:3857"))
    }

    def buildNode(Map args) {
        def node = new Node(args.id)
        node.setCoor(new LatLon(args.lat, args.lon))
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


    @Before
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



