package org.openstreetmap.josm.plugins.contourmerge;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages a set of {@link ContourMergeModel}s for each available data
 * layer.
 * <p>
 * Listens to layer change events and creates new contour merge models
 * for newly added layer, or removes contour merge models, if a layer is deleted.
 */
public class ContourMergeModelManager implements LayerChangeListener{

    static private ContourMergeModelManager instance;
    static public ContourMergeModelManager getInstance() {
        if (instance == null){
            instance = new ContourMergeModelManager();
        }
        return instance;
    }

    private final Map<OsmDataLayer, ContourMergeModel> models =
            new HashMap<>();

    public void wireToJOSM(){
        models.clear();
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    public void unwireFromJOSM() {
        models.clear();
        MainApplication.getLayerManager().removeLayerChangeListener(this);
    }

    /**
     * Replies the contour merge model for the data layer {@code layer},
     * or null, if no such model exists.
     *
     * @param layer the data layer. Must not be null.
     * @return the model
     */
    public Optional<ContourMergeModel> getModel(@NotNull OsmDataLayer layer){
        Objects.requireNonNull(layer);
        return Optional.ofNullable(models.get(layer));
    }

    /**
     * Replies the contour model for the currently active data layer
     * (the "edit layer"), or null, if the currently active layer isn't
     * a data layer.
     *
     * @return the model
     */
    public Optional<ContourMergeModel> getActiveModel() {
        return Optional.ofNullable(MainApplication.getLayerManager()
            .getEditLayer()).flatMap(this::getModel);
    }

    /* --------------------------------------------------------------------- */
    /* interface LayerChangeListener                                         */
    /* --------------------------------------------------------------------- */

    @Override
    public void layerAdded(LayerAddEvent event) {
        Layer newLayer = event.getAddedLayer();
        if (! (newLayer instanceof OsmDataLayer dl)) return;
        final var model = new ContourMergeModel(dl);
        dl.data.addDataSetListener(model);
        models.put(dl, model);
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent event) {
        /* ignore */
    }

    @Override
    public void layerRemoving(LayerRemoveEvent event) {
        Layer oldLayer = event.getRemovedLayer();
        if (! (oldLayer instanceof OsmDataLayer dl)) return;
        ContourMergeModel model = models.get(dl);
        dl.data.removeDataSetListener(model);
        models.remove(dl);
    }
}
