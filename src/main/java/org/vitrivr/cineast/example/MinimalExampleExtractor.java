package org.vitrivr.cineast.example;

import java.util.function.Supplier;

import org.vitrivr.cineast.core.data.FloatVector;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;
import org.vitrivr.cineast.core.db.PersistencyWriter;
import org.vitrivr.cineast.core.db.PersistencyWriterSupplier;
import org.vitrivr.cineast.core.db.PersistentTuple;
import org.vitrivr.cineast.core.features.extractor.Extractor;
import org.vitrivr.cineast.core.setup.EntityCreator;

public class MinimalExampleExtractor implements Extractor {

  /*
   * This is the name of the entity in the persistent storage layer which is used by this Extractor.
   * We use the same name here as in our minimal retriever example. By convention, they are prefixed
   * with 'feature_'.
   */
  private static final String ENTITY_NAME = "features_MinimalExampleExtractorRetriever";

  /*
   * This instance of PersistencyWriter can be used to write data to the storage layer.
   */
  private PersistencyWriter<?> pwriter;

  /*
   * This method is used to initialise the entity in the persistent storage layer to which this
   * extractor is supposed to write.
   */
  @Override
  public void initalizePersistentLayer(Supplier<EntityCreator> supply) {
    try(EntityCreator creator = supply.get()){
      // Here we create a default feature entity which consists of an id and a feature vector. Since
      // this extractor produces at most one feature vector per segment, we set the unique flag to
      // true. This method is called during the setup procedure.
      creator.createFeatureEntity(ENTITY_NAME, true);
    }
  }

  /*
   * This method is used to delete the the entity used by this extractor from the persistent storage
   * layer. The method can be called during the setup process.
   */
  @Override
  public void dropPersistentLayer(Supplier<EntityCreator> supply) {
    try(EntityCreator creator = supply.get()){
      creator.dropEntity(ENTITY_NAME);
    }
  }

  /*
   * This method is used to initialise the extractor with a connection to the storage layer. It
   * is called at the beginning of every new extraction.
   */
  @Override
  public void init(PersistencyWriterSupplier pwriterSupply) {
    this.pwriter = pwriterSupply.get();
    this.pwriter.open(ENTITY_NAME);
  }

  /*
   * This method is used during extraction to transform an incoming segment into its feature
   * representation and persist this representation in the storage layer. Commonly this results in
   * one vector per container but other mappings are possible as well.
   */
  @Override
  public void processShot(SegmentContainer sc) {
    // We first check if the segment is already present in the storage layer. If so, no further
    // extraction needs to be performed.
    if (this.pwriter.idExists(sc.getId())) {
      return;
    }

    // We use the externally defined feature transformation to get to the vector representation.
    FloatVector feature = ExampleFeatureTransform.dominantChannelHistogram(sc);

    // In order to be able to write to the persistent storage layer, we first need to generate a
    // PersistentTuple which holds the properties expected by the entity in the storage layer. In
    // this case, it expects an id to identify the segment and a feature vector. The PersistentTuple
    // can then be written to the storage layer using the PersistencyWriter.
    PersistentTuple tuple = this.pwriter.generateTuple(sc.getId(), feature);
    this.pwriter.persist(tuple);

  }

  /*
   * This method is called at the end of every extraction and is used to free all resources used by
   * the extractor.
   */
  @Override
  public void finish() {
    if (this.pwriter != null) {
      this.pwriter.close();
      this.pwriter = null;
    }
  }

}
