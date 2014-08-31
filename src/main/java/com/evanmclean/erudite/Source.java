package com.evanmclean.erudite;

import java.io.IOException;

/**
 * A source of articles (such as Instapaper).
 *
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface Source
{
  /**
   * List of articles to process.
   *
   * @return List of articles to process.
   * @throws IOException
   */
  Articles getArticles() throws IOException;

  /**
   * The name of the source.
   *
   * @return The name of the source.
   */
  String getName();

  /**
   * Little piece of HTML that is plugged into the footer of our documents to
   * indicate what source we retrieved the articles via.
   *
   * @return Small HTML string indicating the source we retrieved the artices
   *         via.
   */
  String getViaHtml();

  /**
   * Perform an action on an article that was successfully processed (e.g.,
   * delete it from the source, archive it, move it to another folder.)
   *
   * @param article
   *        Article to post-process.
   * @throws IOException
   */
  void onComplete( Article article ) throws IOException;

  /**
   * Perform an action on an article when an error occured while processing it
   * (e.g., move it to another folder to be looked at later.)
   *
   * @param article
   *        Article to post-process.
   * @throws IOException
   */
  void onError( Article article ) throws IOException;
}
