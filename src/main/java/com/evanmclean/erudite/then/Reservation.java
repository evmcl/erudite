package com.evanmclean.erudite.then;

import java.io.File;

/**
 * Reservation of a file and/or folder to store the processed document file(s).
 * Sometimes we need to know the final name of the file and/or folder to be
 * produced before we actually commence processing the article.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface Reservation
{
  /**
   * Removes the file and/or folder that were reserved. Call this if there was
   * an error while processing the article or when calling
   * {@link Then#then(Reservation, File, File)}.
   */
  void cleanup();

  /**
   * The base name (sans suffixes) of the unique file and/or folder that has
   * been reserved.
   * 
   * @return The base name (sans suffixes) of the unique file and/or folder that
   *         has been reserved.
   */
  String getBaseName();

  /**
   * The file that was reserved or <code>null</code>.
   * 
   * @return The file that was reserved or <code>null</code>.
   */
  File getFile();

  /**
   * The name of the file that was reserved or <code>null</code>.
   * 
   * @return The name of the file that was reserved or <code>null</code>.
   */
  String getFileName();

  /**
   * The folder that was reserved or <code>null</code>.
   * 
   * @return The folder that was reserved or <code>null</code>.
   */
  File getFolder();

  /**
   * The name of the folder that was reserved or <code>null</code>.
   * 
   * @return The name of the folder that was reserved or <code>null</code>.
   */
  String getFolderName();
}
