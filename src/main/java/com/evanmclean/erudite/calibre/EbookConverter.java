package com.evanmclean.erudite.calibre;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.misc.ProcessOutputSlurper;
import com.evanmclean.erudite.misc.Utils;
import com.evanmclean.evlib.exceptions.UnhandledException;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.Colls;
import com.google.common.collect.ImmutableList;

/**
 * Uses the <code><a
 * href="http://manual.calibre-ebook.com/cli/ebook-convert.html">ebook-convert</a></code>
 * command to convert a document (usually HTML) to another format (e.g. epub or
 * mobi).
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class EbookConverter
{
  private static final Logger log = LoggerFactory
      .getLogger(EbookConverter.class);

  /**
   * Will find the <code>ebook-convert</code> executable by searching the
   * operating system path and looking in a few other typical locations.
   * 
   * @return The full path to the <code>ebook-convert</code> executable.
   * @throws IllegalStateException
   *         If could not find the executable.
   */
  public static String findEbookConvert()
  {
    return CalCommon.findExe(Utils.IS_WINDOWS ? "ebook-convert.exe"
        : "ebook-convert");
  }

  private final String exePath;
  private final ImmutableList<String> options;

  /**
   * Creates a document converter. Uses {@link #findEbookConvert()} to find the
   * <code>ebook-convert</code> executable.
   */
  public EbookConverter()
  {
    this(null, null, null, null);
  }

  /**
   * Creates a document converter. Uses {@link #findEbookConvert()} to find the
   * <code>ebook-convert</code> executable.
   * 
   * @param output_profile
   *        The argument for the
   *        <code><a href="http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--output-profile">--output-profile</a></code>
   *        argument (e.g. &ldquo;<code>nook</code>&rdquo;). Use
   *        <code>null</code> or an empty string to not include.
   * @param author
   *        The author for the article (see
   *        <code><a href="http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--authors">--authors</a></code>
   *        option.) Use <code>null</code> or an empty string to not include.
   */
  public EbookConverter( final String output_profile, final String author )
  {
    this(null, output_profile, author, null);
  }

  /**
   * Creates a document converter. Uses {@link #findEbookConvert()} to find the
   * <code>ebook-convert</code> executable.
   * 
   * @param output_profile
   *        The argument for the
   *        <code><a href="http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--output-profile">--output-profile</a></code>
   *        argument (e.g. &ldquo;<code>nook</code>&rdquo;). Use
   *        <code>null</code> or an empty string to not include.
   * @param author
   *        The author for the article (see
   *        <code><a href="http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--authors">--authors</a></code>
   *        option.) Use <code>null</code> or an empty string to not include.
   * @param other_options
   *        Extra command line options to be passed to
   *        <code>ebook-convert</code> when converting a document. See the <a
   *        href= "http://manual.calibre-ebook.com/cli/ebook-convert.html"
   *        >Calibre User Manual</a> for documentation on the
   *        <code>calibredb add</code> command and options.
   */
  public EbookConverter( final String output_profile, final String author,
      final List<String> other_options )
  {
    this(null, output_profile, author, other_options);
  }

  /**
   * Creates a document converter.
   * 
   * @param exe_path
   *        The full path to the <code>ebook-convert</code> executable. (Uses
   *        {@link #findEbookConvert()} if an empty string or <code>null</code>
   *        .)
   * @param output_profile
   *        The argument for the
   *        <code><a href="http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--output-profile">--output-profile</a></code>
   *        argument (e.g. &ldquo;<code>nook</code>&rdquo;). Use
   *        <code>null</code> or an empty string to not include.
   * @param author
   *        The author for the article (see
   *        <code><a href="http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--authors">--authors</a></code>
   *        option.) Use <code>null</code> or an empty string to not include.
   * @param other_options
   *        Extra command line options to be passed to
   *        <code>ebook-convert</code> when converting a document. See the <a
   *        href= "http://manual.calibre-ebook.com/cli/ebook-convert.html"
   *        >Calibre User Manual</a> for documentation on the
   *        <code>calibredb add</code> command and options.
   */
  public EbookConverter( final String exe_path, final String output_profile,
      final String author, final List<String> other_options )
  {
    if ( Str.isNotEmpty(exe_path) )
      this.exePath = exe_path;
    else
      this.exePath = findEbookConvert();

    final ImmutableList.Builder<String> bldr = ImmutableList.builder();

    if ( Str.isNotEmpty(output_profile) )
      bldr.add("--output-profile=" + output_profile);

    if ( Str.isNotEmpty(author) )
      bldr.add("--authors=" + author);

    if ( other_options != null )
      bldr.addAll(other_options);

    this.options = bldr.build();
  }

  /**
   * Converts a document (usually HTML) to another format (e.g. epub or mobi)
   * using <code>ebook-convert</code>.
   * 
   * @param title
   *        An optional title (use an empty string or <code>null</code> for
   *        unspecified or to use whatever is in the <code>file</code> (if
   *        anything).) See <code><a href=
   *        "http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--title"
   *        >--title</a></code> in the Calibre documentation.
   * @param in_file
   *        The file to be converted.
   * @param out_file
   *        The target file.
   * @param summary
   *        An optional summary of the contents of the document, in HTML (use an
   *        empty string or <code>null</code> for unspecified or to use whatever
   *        is in the <code>file</code> (if anything).) See <code><a href=
   *        "http://manual.calibre-ebook.com/cli/ebook-convert.html#cmdoption-ebook-convert--comments"
   *        >--comments</a></code> in the Calibre documentation.
   * @throws IOException
   */
  public void convert( final String title, final File in_file,
      final File out_file, final String summary ) throws IOException
  {
    final List<String> cmd = Colls.newArrayList(10);
    cmd.add(exePath);
    cmd.add(in_file.toString());
    cmd.add(out_file.toString());
    if ( Str.isNotEmpty(title) )
      cmd.add("--title=" + title);
    if ( Str.isNotEmpty(summary) )
      cmd.add("--comments=" + summary);
    cmd.addAll(options);

    log.debug("Running: {}", Str.join(", ", cmd));
    final Process proc = new ProcessBuilder(cmd).redirectErrorStream(true)
        .start();

    final ProcessOutputSlurper slurper = new ProcessOutputSlurper(proc);

    final int ret;
    try
    {
      ret = proc.waitFor();
    }
    catch ( InterruptedException ex )
    {
      throw new UnhandledException(ex);
    }

    final List<String> output = slurper.getLines();

    if ( ret != 0 )
      log.trace("ebook-convert returned {}", ret);

    log.trace("ebook-convert output:");
    for ( String line : output )
      log.trace(line);

    if ( ret == 0 )
    {
      final Pattern pat = Pattern.compile("\\S+ output written to .+$");
      for ( String line : output )
        if ( pat.matcher(line).matches() )
          return;
    }

    throw new IOException(
        "Error converting document. See log file for details.");
  }
}
