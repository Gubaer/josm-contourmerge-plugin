package org.openstreetmap.josm.plugins.contourmerge

import groovy.test.GroovyTestCase
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openstreetmap.josm.actions.OpenFileAction
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.Preferences
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.projection.ProjectionRegistry
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.layer.OsmDataLayer
import org.openstreetmap.josm.spi.preferences.Config

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test case for Issue 21,
 *  {@see https://github.com/Gubaer/josm-contourmerge-plugin/issues/21}
 */
class DuplicateNodeTest extends GroovyTestCase {

    DataSet dataSet
    OsmDataLayer layer
    ContourMergeModel mergeModel

    @BeforeAll
    static void initJosmConfig() {
        Config.setPreferencesInstance(new Preferences())
        ProjectionRegistry.setProjection(
                Projections.getProjectionByCode("EPSG:3857"))
    }

    def createDataSet() {
        def files = [new File("test/data/issue-21/sample.osm")]
        OpenFileAction.openFiles(files).get()
        layer = MainApplication.layerManager.getLayers().find {
            it.name == "sample.osm"
        }
        assertThat(layer, is(notNullValue()))
        dataSet=layer.dataSet
    }

    def prepareMergeModel() {
        mergeModel = new ContourMergeModel(layer)
    }

    @BeforeEach
    void prepareTestData() {
        createDataSet()
        prepareMergeModel()
    }

    @Test
    void "merge should not produce way with sequences of identical nodes - 01"() {

        def sourceWay = dataSet.getPrimitiveById(100, OsmPrimitiveType.WAY) as Way
        assertThat(sourceWay, is(notNullValue()))

        def targetWay = dataSet.getPrimitiveById(200, OsmPrimitiveType.WAY) as Way
        assertThat(targetWay, is(notNullValue()))

        def sourceWaySlice = new WaySlice(sourceWay, 1, 2)
        def targetWaySlice = new WaySlice(targetWay, 0, 1)

        def mergeCommand = mergeModel.buildContourAlignCommand(
            sourceWaySlice,
            targetWaySlice
        ) as SequenceCommand

        UndoRedoHandler.getInstance().add(mergeCommand)

        sourceWay = dataSet.getPrimitiveById(100, OsmPrimitiveType.WAY) as Way
        def newWayNodeIds = sourceWay.nodes.collect {it.primitiveId.uniqueId}

        List<Long> expectedSourceWayNodeIds = [1001,2001,2002,1004,1005,1006,1007,1001]

        assertThat("merged sourceWay consists of the expected nodes",
            newWayNodeIds,
            anyOf(
                new IsIdenticalListOfLongs(expectedSourceWayNodeIds),
                new IsIdenticalListOfLongs(expectedSourceWayNodeIds.reverse())
            )
        )
    }
}

class IsIdenticalListOfLongs extends TypeSafeMatcher<List<Long>> {

    private List<Long> expected

    IsIdenticalListOfLongs(List<Long> expected) {
        this.expected = expected
    }

    @Override
    protected boolean matchesSafely(List<Long> items) {
        if (expected.size() != items.size()) {
            return false
        }
        return (0..expected.size()-1).every {i ->
            expected[i] == items[i]
        }
    }

    @Override
    void describeTo(Description description) {
        description.append("is identical list of integers")
    }
}

