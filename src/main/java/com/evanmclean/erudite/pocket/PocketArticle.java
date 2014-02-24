package com.evanmclean.erudite.pocket;

import java.io.IOException;
import java.util.List;

import com.evanmclean.erudite.Article;

/**
 * An {@link Article} from Pocket with some additional functionality.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface PocketArticle extends Article
{
  /**
   * Add a comma separated list of tags to the article in Pocket.
   * 
   * @param tags
   * @throws IOException
   */
  void addTags( List<String> tags ) throws IOException;

  /**
   * Archive the article on Pocket.
   * 
   * @throws IOException
   */
  void archive() throws IOException;

  /**
   * Sets the favourite flag on the article in Pocket.
   * 
   * @param favourite
   *        Set or unset the flag.
   * @throws IOException
   */
  void favourite( boolean favourite ) throws IOException;

  /**
   * Remove the article from Pocket.
   * 
   * @throws IOException
   */
  void remove() throws IOException;

  /**
   * Remove tags from an article in Pocket.
   * 
   * @param tags
   * @throws IOException
   */
  void removeTags( List<String> tags ) throws IOException;
}
