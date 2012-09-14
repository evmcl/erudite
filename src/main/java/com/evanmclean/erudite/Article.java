package com.evanmclean.erudite;

import java.io.IOException;

import org.jsoup.nodes.Element;

/**
 * A single article (e.g., from Instapaper) that is to be processed.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface Article
{
  /**
   * The original URL of the article.
   * 
   * @return The original URL of the article.
   */
  String getOriginalUrl();

  /**
   * Some sources of articles (such as Instapaper) provide a way to see any web
   * page formatted by their service (even if you haven't saved it in your
   * article list). This will be returned here if available.
   * 
   * @return A URL to view the article via the Source (such as Instapaper), or
   *         <code>null</code> if not available.
   */
  String getSourceUrl();

  /**
   * A plain text summary of the article, if available. May be an empty string
   * or <code>null</code> if none available.
   * 
   * @return The summary, and empty string or <code>null</code>.
   */
  String getSummary();

  /**
   * The title of the article.
   * 
   * @return The title of the article.
   */
  String getTitle();

  /**
   * <p>
   * The HTML text of article. This is not a full HTML document, just the
   * content.
   * </p>
   * 
   * <p>
   * Developers note: If you cache the text, and don't fetch it from the source
   * each time this method is called, then you should return a copy that it is
   * safe for the calling code to modify.
   * </p>
   * 
   * @return An {@link Element} containing the text of the article.
   * @throws IOException
   */
  Element text() throws IOException;
}
