package org.openstreetmap.josm.plugins.contourmerge
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.openstreetmap.josm.data.validation.tests.DuplicatedWayNodes

@RunWith(Suite.class)
@Suite.SuiteClasses([
    ContourMergeModelTest.class,
    WaySliceTest.class,
    DataIntegrityProblemTest01.class,
    DataIntegrityProblemTest02.class,
    DuplicateNodeTest.class
])
class AllUnitTests {}
