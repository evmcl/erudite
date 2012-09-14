package com.evanmclean.erudite;

import java.io.IOException;

/**
 * Deals with an image based on the source URL. Usually used to download the
 * image to a local file to aid conversion.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface ImageHandler
{
  /**
   * Deals with an image based on the source URL. Usually used to download the
   * image to a local file to aid conversion.
   * 
   * @param source
   *        Source URL of the image.
   * @return Returns the rewritten (probably relative) image source URL to be
   *         written back to the HTML file, or <code>null</code> if the image
   *         could not be handled.
   * @throws IOException
   */
  String image( String source ) throws IOException;
}
