package org.vitrivr.cineast.example;

import java.util.List;

import org.vitrivr.cineast.core.config.ReadableQueryConfig;
import org.vitrivr.cineast.core.data.FloatVector;
import org.vitrivr.cineast.core.data.ReadableFloatVector;
import org.vitrivr.cineast.core.data.score.ScoreElement;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;
import org.vitrivr.cineast.core.features.abstracts.AbstractFeatureModule;

public class MinimalExampleFeatureModule extends AbstractFeatureModule {

  /*
   * This is the name of the entity in the persistent storage layer which is used by this module. By
   * convention, they are prefixed with 'feature_'.
   */
  private static final String ENTITY_NAME = "features_MinimalExampleFeatureModule";

  /*
   * Depending on the used correspondence function, there needs to be a maximal distance after which
   * the similarity between two vectors is to be considered 0.
   */
  private static final float MAX_DISTANCE = 1f;

  public MinimalExampleFeatureModule() {
    super(ENTITY_NAME, MAX_DISTANCE);
  }

  /*
   * This method is used during extraction to transform an incoming segment into its feature
   * representation and persist this representation in the storage layer. Commonly this results in
   * one vector per container but other mappings are possible as well.
   */
  @Override
  public void processSegment(SegmentContainer sc) {
    // We first check if the segment is already present in the storage layer. If so, no further
    // extraction needs to be performed.
    if (phandler.idExists(sc.getId())) {
      return;
    }

    // We use the externally defined feature transformation to get to the vector representation.
    FloatVector feature = ExampleFeatureTransform.dominantChannelHistogram(sc);

    // This is a helper method which persists the vector representation in the storage layer.
    persist(sc.getId(), feature);

  }

  /*
   * This method is used for content-based retrieval. It has to perform a transformation compatible
   * to the one above.
   */
  @Override
  public List<ScoreElement> getSimilar(SegmentContainer sc, ReadableQueryConfig qc) {

    // We use the exact same feature transform for the query as for the extraction.
    FloatVector queryVector = ExampleFeatureTransform.dominantChannelHistogram(sc);
    float[] queryFloatArray = ReadableFloatVector.toArray(queryVector);

    // Since this is a standard nearest-neighbour search, we can use the helper method to handle the
    // request. It will perform the query on the storage layer and return the results in the
    // expected format.
    return getSimilar(queryFloatArray, qc);

  }

}
