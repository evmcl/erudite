package com.evanmclean.erudite.instapaper;

import java.io.IOException;

import com.evanmclean.erudite.Article;

/**
 * An {@link Article} from Instapaper with some additional functionality.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface InstapaperArticle extends Article
{
  /**
   * Archive the article on Instapaper.
   * 
   * @throws IOException
   */
  void archive() throws IOException;

  /**
   * Move the article to another folder on Instapaper.
   * 
   * @param folder
   * @throws IOException
   */
  void move( InstapaperFolder folder ) throws IOException;

  /**
   * Remove the article from Instapaper.
   * 
   * @throws IOException
   */
  void remove() throws IOException;
}
