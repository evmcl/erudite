package com.evanmclean.erudite;

import java.io.File;

import com.evanmclean.erudite.config.ProcessorsFactory;

/**
 * Processes an article. See the user documentation (in particular, the sample
 * configuration file) for what these can do.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 * @see ProcessorsFactory
 */
public interface Processor
{
  /**
   * Processes an article. See the user documentation (in particular, the sample
   * configuration file) for what this can do.
   * 
   * @param article
   *        The article to process.
   * @param erudite
   *        An {@link Erudite} object for formatting the HTML document.
   * @param source
   *        The source of the article.
   * @param ihf
   *        An image factory handler.
   * @param work_folder
   *        A temporary folder that can use to do all its processing.
   * @throws Exception
   */
  void process( Article article, Erudite erudite, Source source,
      ImageHandlerFactory ihf, File work_folder ) throws Exception;
}
