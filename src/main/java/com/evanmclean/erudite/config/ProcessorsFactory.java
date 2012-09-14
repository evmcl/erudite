package com.evanmclean.erudite.config;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.Processor;
import com.evanmclean.erudite.TemplateFactory;
import com.evanmclean.erudite.calibre.CalibreAddProcessor;
import com.evanmclean.erudite.calibre.EbookConvertProcessor;
import com.evanmclean.erudite.save.SaveProcessor;
import com.evanmclean.evlib.exceptions.UnhandledException;
import com.evanmclean.evlib.lang.Str;
import com.google.common.collect.ImmutableList;

/**
 * Reads the list of {@link Processor}s specified by a {@link Config} and
 * instantiates them, properly configured.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class ProcessorsFactory
{
  /**
   * Reads the comma separated list of processors from the
   * <code>processors</code> key in the configuration, and instantiates them.
   * 
   * @param config
   *        The configuration to read.
   * @param tf
   *        A template factory.
   * @return A list of processors.
   */
  public static ImmutableList<Processor> get( final Config config,
      final TemplateFactory tf )
  {
    final List<String> configured_processors = parseProcessorList(config
        .getString("processors", Str.EMPTY));

    final ImmutableList.Builder<Processor> procs = ImmutableList.builder();

    for ( String name : configured_processors )
      procs.add(getProcessor(name, config, tf));

    return procs.build();
  }

  private static Processor getProcessor( final String key, final Config config,
      final TemplateFactory tf )
  {
    final String prefix = key + '.';
    final String type = config.getString(prefix + "type");
    if ( Str.isEmpty(type) )
      throw new IllegalStateException(
          "Configuration missing processor type for " + prefix + "type");

    try
    {
      if ( Str.equalsOneOfIgnoreCase(type, "ebookconvert", "ebook-convert") )
        return new EbookConvertProcessor(prefix, config, tf);
      if ( Str.equalsOneOfIgnoreCase(type, "calibre", "calibredb") )
        return new CalibreAddProcessor(prefix, config, tf);
      if ( Str.equalsOneOfIgnoreCase(type, "save", "saveto") )
        return new SaveProcessor(prefix, config, tf);
    }
    catch ( IOException ex )
    {
      throw new UnhandledException(ex);
    }

    throw new IllegalStateException("Unknown processor type: " + type);
  }

  private static ImmutableList<String> parseProcessorList( final String str )
  {
    final TreeSet<String> list = new TreeSet<String>();
    try
    {
      final Pattern regex = Pattern.compile("[a-z][a-z0-9]*",
        Pattern.CASE_INSENSITIVE);
      final Matcher mat = regex.matcher(str);
      final int len = Str.length(str);
      int pos = 0;
      while ( pos < len )
      {
        if ( str.charAt(pos) == ',' )
          ++pos;

        while ( (pos < len) && Character.isWhitespace(str.charAt(pos)) )
          ++pos;
        if ( pos < len )
        {
          final int begin = mat.find(pos) ? mat.start() : -1;
          if ( begin != pos )
            throw new IllegalArgumentException("Parse error as position " + pos
                + ": " + str);
          pos = mat.end();
          list.add(mat.group());
        }
      }
    }
    catch ( IllegalArgumentException ex )
    {
      LoggerFactory.getLogger(ProcessorsFactory.class).trace(
        "Invalid list of processors: " + str, ex);
      throw new IllegalArgumentException("Invalid list of processors: " + str);
    }
    if ( list.isEmpty() )
      throw new IllegalArgumentException(
          "No list of processors specified in configuration file.");
    return ImmutableList.copyOf(list);
  }

  private ProcessorsFactory()
  {
    // empty
  }
}
