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
import com.evanmclean.erudite.misc.EpubWrapHack;
import com.evanmclean.erudite.misc.Utils;
import com.evanmclean.erudite.then.Reservation;
import com.evanmclean.erudite.then.Then;
import com.evanmclean.erudite.then.ThenFactory;
import com.evanmclean.evlib.lang.Str;

/**
 * A processor for converting HTML documents to another format (such as epub or
 * mobi) and then adding them to a Calibre e&ndash;book library or saving them
 * in a folder. See the user documentation (in particular the sample
 * configuration file).
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class EbookConvertProcessor implements Processor
{
  private final EbookConverter converter;
  private final Template template;
  private final String filetype;
  private final String fileSuffix;
  private final boolean doFootnotes;
  private final boolean doHNSearch;
  private final boolean doWrapHack;
  private final Then then;

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
  public EbookConvertProcessor( final String prefix, final Config config,
      final TemplateFactory tf ) throws IOException
  {
    // File type
    this.filetype = Str.ifNull(config.getString(prefix + "filetype"))
        .toLowerCase();
    if ( Str.isEmpty(this.filetype) )
      throw new IllegalStateException("No file type specified: " + prefix
          + ".filetype");
    fileSuffix = "." + filetype;

    // Wrap Hack
    this.doWrapHack = Str.notEqualsIgnoreCase(this.filetype, "epub") ? false
        : config.getBoolean(prefix + "wrap.hack", false);

    // Footnotes
    this.doFootnotes = ConfigUtils.toBoolean(
      ConfigUtils.getFirst(config, prefix + "footnotes", "footnotes"), true);

    // Hacker News Search
    this.doHNSearch = ConfigUtils.toBoolean(
      ConfigUtils.getFirst(config, prefix + "hnsearch", "hnsearch"), false);

    // Program path
    final String exe;
    {
      String str = ConfigUtils.getFirst(config, "ebookconvert.prog",
        "ebookconvert");
      exe = (str != null) ? str : EbookConverter.findEbookConvert();
    }

    // Converter
    final String output_profile = ConfigUtils.getFirst(config, prefix
        + "outputprofile", "ebookconvert.outputprofile");

    final String author = config.getString(prefix + "author");

    this.converter = new EbookConverter(exe, output_profile, author //
        , config.getStrings(prefix + "option"));

    // Template
    this.template = tf.get(ConfigUtils.getFirst(config, prefix + "template",
      "template"));

    // Then
    this.then = ThenFactory.get(prefix, config);
  }

  @Override
  public void process( final Article article, final Erudite erudite,
      final Source source, final ImageHandlerFactory ihf, final File work_folder )
    throws Exception
  {
    boolean okay = false;
    final Reservation reservation = then.reserve(article, fileSuffix, null);
    try
    {
      // Save as HTML
      final File html_file = saveAsHtml(article, erudite, source, ihf,
        work_folder);

      // Convert
      final File pub_file = convert(source, article, html_file, work_folder);

      // Then...
      then.then(reservation, pub_file, null);

      okay = true;
    }
    finally
    {
      if ( !okay )
        reservation.cleanup();
    }
  }

  private File convert( final Source source, final Article article,
      final File html_file, final File work_folder ) throws IOException
  {
    final File pub_file = new File(work_folder, "temp"
        + (doWrapHack ? "_hacked." : ".") + filetype);
    converter.convert(article.getTitle(), html_file, pub_file, //
      Utils.summary(source, article, doHNSearch ? HNSearch.lookup(article)
          : null) //
        );

    if ( !doWrapHack )
      return pub_file;

    final File dehacked_file = new File(work_folder, "temp." + filetype);
    EpubWrapHack.postProcess(pub_file, dehacked_file);

    return dehacked_file;
  }

  private File saveAsHtml( final Article article, final Erudite erudite,
      final Source source, final ImageHandlerFactory ihf, final File work_folder )
    throws IOException
  {
    final File html_file = new File(work_folder, "temp.html");

    Document doc = erudite.process(article, source, template,
      ihf.get(work_folder, Str.EMPTY), doFootnotes //
      , doHNSearch ? HNSearch.lookup(article) : null //
        );

    if ( doWrapHack )
      doc = EpubWrapHack.preProcess(doc);

    Doc.write(doc, html_file);

    return html_file;
  }
}
