package com.evanmclean.erudite.save;

import java.io.File;
import java.io.IOException;

import org.jsoup.nodes.Document;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Erudite;
import com.evanmclean.erudite.HNSearch;
import com.evanmclean.erudite.ImageHandlerFactory;
import com.evanmclean.erudite.Processor;
import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.Template;
import com.evanmclean.erudite.TemplateFactory;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.config.ConfigUtils;
import com.evanmclean.erudite.misc.Doc;
import com.evanmclean.erudite.then.Reservation;
import com.evanmclean.erudite.then.SaveThen;
import com.evanmclean.evlib.io.Folders;

/**
 * A {@link Processor} for saving articles as HTML files in a folder. See the
 * user documentation (in particular the sample configuration file).
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class SaveProcessor implements Processor
{
  private final SaveThen fileManager;
  private final Template template;
  private final boolean doFootnotes;
  private final boolean doHNSearch;

  /**
   * Load the processor based on a configuration.
   * 
   * @param prefix
   *        The prefix of the keys to read in the configuration.
   * @param config
   *        The configuration to read.
   * @param tf
   *        Template factory.
   * @throws IOException
   */
  public SaveProcessor( final String prefix, final Config config,
      final TemplateFactory tf ) throws IOException
  {
    // Footnotes
    this.doFootnotes = ConfigUtils.toBoolean(
      ConfigUtils.getFirst(config, prefix + "footnotes", "footnotes"), true);

    // Hacker News Search
    this.doHNSearch = ConfigUtils.toBoolean(
      ConfigUtils.getFirst(config, prefix + "hnsearch", "hnsearch"), false);

    // Save To
    this.fileManager = new SaveThen(prefix, config);

    // Template
    this.template = tf.get(ConfigUtils.getFirst(config, prefix + "template",
      "template"));
  }

  @Override
  public void process( final Article article, final Erudite erudite,
      final Source source, final ImageHandlerFactory ihf, final File work_folder )
    throws Exception
  {
    boolean okay = false;
    final Reservation reservation = fileManager.reserve(article, ".html",
      "_files");
    try
    {
      // Save as HTML
      saveAsHtml(article, erudite, source, ihf, reservation);
      okay = true;
    }
    finally
    {
      if ( !okay )
        reservation.cleanup();
    }
  }

  private void saveAsHtml( final Article article, final Erudite erudite,
      final Source source, final ImageHandlerFactory ihf,
      final Reservation reservation ) throws IOException
  {
    final File html_file = reservation.getFile();
    final File image_folder = reservation.getFolder();

    Document doc = erudite.process(article, source, template, //
      ihf.get(image_folder //
        , reservation.getFolderName() + '/') //
      , doFootnotes //
      , doHNSearch ? HNSearch.lookup(article) : null //
        );

    if ( Folders.isEmpty(image_folder) )
      image_folder.delete();

    Doc.write(doc, html_file);
  }
}
