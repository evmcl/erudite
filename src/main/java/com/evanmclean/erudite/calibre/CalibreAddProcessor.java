package com.evanmclean.erudite.calibre;

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
import com.evanmclean.evlib.stringtransform.FilenameTransformer;

/**
 * A processor for adding documents to a Calibre e&ndash;book library as
 * straight HTML. See the user documentation (in particular the sample
 * configuration file).
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class CalibreAddProcessor implements Processor
{
  private final CalibreAdder adder;
  private final String author;
  private final Template template;
  private final boolean doFootnotes;
  private final boolean doHNSearch;
  private final FilenameTransformer ft = new FilenameTransformer(200);

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
  public CalibreAddProcessor( final String prefix, final Config config,
      final TemplateFactory tf ) throws IOException
  {
    // Footnotes
    this.doFootnotes = ConfigUtils.toBoolean(
      ConfigUtils.getFirst(config, prefix + "footnotes", "footnotes"), true);

    // Hacker News Search
    this.doHNSearch = ConfigUtils.toBoolean(
      ConfigUtils.getFirst(config, prefix + "hnsearch", "hnsearch"), false);

    // Adder
    this.adder = new CalibreAdder( //
        ConfigUtils.getFirst(config, "calibredb.prog", "calibredb") //
        , ConfigUtils.getFirst(config, prefix + "library", "calibredb.library") //
        , config.getStrings(prefix + "option") //
    );

    // Author
    this.author = config.getString(prefix + "author");

    // Template
    this.template = tf.get(ConfigUtils.getFirst(config, prefix + "template",
      "template"));
  }

  @Override
  public void process( final Article article, final Erudite erudite,
      final Source source, final ImageHandlerFactory ihf, final File work_folder )
    throws Exception
  {
    // Save as HTML
    final File html_file = saveAsHtml(article, erudite, source, ihf,
      work_folder);

    // Add
    adder.add(html_file, article.getTitle(), author);
  }

  private File saveAsHtml( final Article article, final Erudite erudite,
      final Source source, final ImageHandlerFactory ihf, final File work_folder )
    throws IOException
  {
    final String base_name = ft.transform(article.getTitle(), "document");
    final File html_file = new File(work_folder, base_name + ".html");

    Document doc = erudite.process(article, source, template, //
      ihf.get(new File(work_folder, base_name + "_files") //
        , base_name + "_files/") //
      , doFootnotes //
      , doHNSearch ? HNSearch.lookup(article) : null //
        );

    Doc.write(doc, html_file);

    return html_file;
  }
}
