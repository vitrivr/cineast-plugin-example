package org.vitrivr.cineast.example;

import org.vitrivr.cineast.core.color.RGBContainer;
import org.vitrivr.cineast.core.data.FloatVector;
import org.vitrivr.cineast.core.data.FloatVectorImpl;
import org.vitrivr.cineast.core.data.frames.VideoFrame;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;

public class ExampleFeatueTransform {

  /*
   * This is a helper method which performs the actual transformation from a SegmentContainer to a
   * vector representation which can be used for extraction and retrieval.
   * 
   * In this example, we build a histogram over all available video-frames and count if they are
   * predominantly red, green or blue.
   */
  public static FloatVector dominantChannelHistogram(SegmentContainer sc) {

    float[] histogram = new float[3];

    for (VideoFrame frame : sc.getVideoFrames()) { // a SegmentContainer has many representation of
                                                   // data present in the multimedia object.
                                                   // Depending on the media type, some may be
                                                   // empty.

      int[] pixels = frame.getImage().getColors();
      for (int pixel : pixels) {
        int red = RGBContainer.getRed(pixel);
        int green = RGBContainer.getGreen(pixel);
        int blue = RGBContainer.getBlue(pixel);
        int alpha = RGBContainer.getAlpha(pixel);

        if (alpha < 128) { // we ignore all pixels with an alpha value less than half. This has the
                           // consequence that transparent regions are ignored which leads to more
                           // flexibility in specifying query images.
          continue;
        }
        
        if(red == green && green == blue){
          continue;
        }
        
        int max = Math.max(Math.max(red, green), blue);
        
        if(red == max && green < max && blue < max){
          histogram[0] += 1;
        }
        
        else if(red < max && green == max && blue < max){
          histogram[1] += 1;
        }
        
        else if(red < max && green < max && blue == max){
          histogram[2] += 1;
        }

      }
    }
    
    //Normalise histogram
    float sum = histogram[0] + histogram[1] + histogram[2];
    if(sum > 0f){
      histogram[0] /= sum;
      histogram[1] /= sum;
      histogram[2] /= sum;
    }

    return new FloatVectorImpl(histogram);

  }
  
}
