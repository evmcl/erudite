package com.evanmclean.erudite.instapaper;

import java.io.IOException;

import com.google.common.collect.ImmutableList;

/**
 * A folder on Instapaper.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface InstapaperFolder
{
  /**
   * A list of the articles contained in the folder.
   * 
   * @return A list of the articles contained in the folder (will be an empty
   *         list if there are no articles).
   * @throws IOException
   */
  ImmutableList<InstapaperArticle> getArticles() throws IOException;

  /**
   * The name of the folder.
   * 
   * @return The name of the folder.
   */
  String getName();
}
