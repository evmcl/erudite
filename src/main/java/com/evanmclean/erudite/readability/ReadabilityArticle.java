package com.evanmclean.erudite.readability;

import java.io.IOException;

import com.evanmclean.erudite.Article;

/**
 * An {@link Article} from Readability with some additional functionality.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface ReadabilityArticle extends Article
{
  /**
   * Add a comma separated list of tags to the article in Readability.
   * 
   * @param tags
   * @throws IOException
   */
  void addTags( String tags ) throws IOException;

  /**
   * Archive the article on Readability.
   * 
   * @throws IOException
   */
  void archive() throws IOException;

  /**
   * Sets the favourite flag on the article in Readability.
   * 
   * @param favourite
   *        Set or unset the flag.
   * @throws IOException
   */
  void favourite( boolean favourite ) throws IOException;

  /**
   * Remove the article from Readability.
   * 
   * @throws IOException
   */
  void remove() throws IOException;

  /**
   * Remove a tag from an article in Readability.
   * 
   * @param tag
   * @throws IOException
   */
  void removeTag( String tag ) throws IOException;
}
