package org.vitrivr.cineast.example;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.vitrivr.cineast.core.config.Config;
import org.vitrivr.cineast.core.config.ReadableQueryConfig;
import org.vitrivr.cineast.core.data.CorrespondenceFunction;
import org.vitrivr.cineast.core.data.FloatVector;
import org.vitrivr.cineast.core.data.ReadableFloatVector;
import org.vitrivr.cineast.core.data.distance.DistanceElement;
import org.vitrivr.cineast.core.data.distance.SegmentDistanceElement;
import org.vitrivr.cineast.core.data.score.ScoreElement;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;
import org.vitrivr.cineast.core.db.DBSelector;
import org.vitrivr.cineast.core.db.DBSelectorSupplier;
import org.vitrivr.cineast.core.features.retriever.Retriever;
import org.vitrivr.cineast.core.setup.EntityCreator;

public class MinimalExampleRetriever implements Retriever {

  /*
   * This is the name of the entity in the persistent storage layer which is used by this Retriever.
   * We use the same name here as in our minimal extractor example. By convention, they are prefixed
   * with 'feature_'.
   */
  private static final String ENTITY_NAME = "features_MinimalExampleExtractorRetriever";

  /*
   * This instance of DBSelector can be used to query the storage layer.
   */
  private DBSelector selector;

  /*
   * This method is used to initialise the entity in the persistent storage layer from which this
   * retriever is supposed to read.
   */
  @Override
  public void initalizePersistentLayer(Supplier<EntityCreator> supply) {
    try(EntityCreator creator = supply.get()){
      // Here we create a default feature entity which consists of an id and a feature vector. This
      // method is called during the setup procedure.
      creator.createFeatureEntity(ENTITY_NAME, true);
    }
  }

  /*
   * This method is used to delete the the entity used by this retriever from the persistent storage
   * layer. The method can be called during the setup process.
   */
  @Override
  public void dropPersistentLayer(Supplier<EntityCreator> supply) {
    try(EntityCreator creator = supply.get()){
      creator.dropEntity(ENTITY_NAME);
    }
  }

  /*
   * This method is used to initialise the retriever with a connection to the storage layer. It is
   * called at the beginning of every query.
   */
  @Override
  public void init(DBSelectorSupplier selectorSupply) {
    this.selector = selectorSupply.get();
    this.selector.open(ENTITY_NAME);
  }

  /*
   * This method is used for content-based retrieval. It has to perform a transformation compatible
   * to the one used during extraction. The provided SegmentContainer contains the relevant query
   * information
   */
  @Override
  public List<ScoreElement> getSimilar(SegmentContainer sc, ReadableQueryConfig qc) {
    // We use the exact same feature transform for the query as for the extraction.
    FloatVector queryVector = ExampleFeatureTransform.dominantChannelHistogram(sc);
    float[] queryFloatArray = ReadableFloatVector.toArray(queryVector);

    // We use the DBSelector to perform a nearest neighbour query on the storage layer.
    List<SegmentDistanceElement> distances = this.selector.getNearestNeighbours(
        // The number of results requested is commonly identical to the value set in the configuration
        Config.sharedConfig().getRetriever().getMaxResultsPerModule(),
        queryFloatArray, // We use the previously computed vector as a query-vector
        "feature", // By default, the property which holds the vectors is called 'feature'
        // Specify the type of results that we expect, that is either segments
        // or entire multimedia objects.
        SegmentDistanceElement.class,
        // The QueryConfig holds additional settings to be used during the lookup. It specifies
        // for instance, which distance function is to be used.
        qc 
       );
    
    // Since the DBSelector returned elements describing the distance to the specified query vector
    // with respect to the specified distance function, a CorrespondenceFunction needs to be applied
    // which transforms the distances into scores. Here we check if the QueryConfig specifies a 
    // function to be used. If not, we use a linear correspondence with 1f as a maximum distance.
    CorrespondenceFunction function = qc.getCorrespondenceFunction()
        .orElse(CorrespondenceFunction.linear(1f));
    
    // The CorrespondenceFunction is then used to produce the final list which is then returned.
    return DistanceElement.toScore(distances, function);
  }

  @Override
  public List<ScoreElement> getSimilar(String shotId, ReadableQueryConfig qc) {
    // To retrieve segments similar to an already known one, we first need to lookup the relevant
    // feature vector.
    List<float[]> list = this.selector.getFeatureVectors("id", shotId, "feature");
    
    // In case there is no such vector, the result set is empty.
    if (list.isEmpty()) {
      return new ArrayList<>(0);
    }

    // After this lookup, the same query as above is performed.
    List<SegmentDistanceElement> distances = this.selector.getNearestNeighbours(
        Config.sharedConfig().getRetriever().getMaxResultsPerModule(),
        list.get(0), // We use the previously retrieved vector as a query-vector
        "feature",
        SegmentDistanceElement.class,
        qc
       );
    CorrespondenceFunction function = qc.getCorrespondenceFunction()
        .orElse(CorrespondenceFunction.linear(1f));
    return DistanceElement.toScore(distances, function);
    
  }

  /*
   * This method is called at the end of every query and is used to free all resources used by the
   * retriever.
   */
  @Override
  public void finish() {
    if (this.selector != null) {
      this.selector.close();
      this.selector = null;
    }
  }

}
