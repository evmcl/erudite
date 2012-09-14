package com.evanmclean.erudite.calibre;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
 * href="http://manual.calibre-ebook.com/cli/calibredb.html">calibredb</a> add</code>
 * command to add a document to an e&ndash;book library.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class CalibreAdder
{
  private static final Logger log = LoggerFactory.getLogger(CalibreAdder.class);

  /**
   * Will find the <code>calibredb</code> executable by searching the operating
   * system path and looking in a few other typical locations.
   * 
   * @return The full path to the <code>calibredb</code> executable.
   * @throws IllegalStateException
   *         If could not find the executable.
   */
  public static String findCalibreDb()
  {
    return CalCommon.findExe(Utils.IS_WINDOWS ? "calibredb.exe" : "calibredb");
  }

  private final String exePath;
  private final ImmutableList<String> options;

  /**
   * Will add a file to the default library in Calibre. Uses
   * {@link #findCalibreDb()} to find the <code>calibredb</code> executable.
   */
  public CalibreAdder()
  {
    this(null, null, null);
  }

  /**
   * Will add a file to the specified library in Calibre. Uses
   * {@link #findCalibreDb()} to find the <code>calibredb</code> executable.
   * 
   * @param library_path
   *        The full path to the library folder in which to store documents.
   *        (Uses the default library if an empty string or <code>null</code>.)
   *        See
   *        <code><a href="http://manual.calibre-ebook.com/cli/calibredb.html#cmdoption--library-path">--library-path</a></code>
   *        in the Calibre documentation.
   */
  public CalibreAdder( final String library_path )
  {
    this(null, library_path, null);
  }

  /**
   * Will add a file to the specified library in Calibre.
   * 
   * @param exe_path
   *        The full path to the <code>calibredb</code> executable. (Uses
   *        {@link #findCalibreDb()} if an empty string or <code>null</code>.)
   * @param library_path
   *        The full path to the library folder in which to store documents.
   *        (Uses the default library if an empty string or <code>null</code>.)
   *        See
   *        <code><a href="http://manual.calibre-ebook.com/cli/calibredb.html#cmdoption--library-path">--library-path</a></code>
   *        in the Calibre documentation.
   */
  public CalibreAdder( final String exe_path, final String library_path )
  {
    this(exe_path, library_path, null);
  }

  /**
   * Will add a file to the specified library in Calibre.
   * 
   * @param exe_path
   *        The full path to the <code>calibredb</code> executable. (Uses
   *        {@link #findCalibreDb()} if an empty string or <code>null</code>.)
   * @param library_path
   *        The full path to the library folder in which to store documents.
   *        (Uses the default library if an empty string or <code>null</code>.)
   *        See
   *        <code><a href="http://manual.calibre-ebook.com/cli/calibredb.html#cmdoption--library-path">--library-path</a></code>
   *        in the Calibre documentation.
   * @param other_options
   *        Extra command line options to be passed to <code>calibredb</code>
   *        when adding a document. See the <a href=
   *        "http://manual.calibre-ebook.com/cli/calibredb.html#calibredb-add"
   *        >Calibre User Manual</a> for documentation on the
   *        <code>calibredb add</code> command and options.
   */
  public CalibreAdder( final String exe_path, final String library_path,
      final List<String> other_options )
  {
    if ( Str.isNotEmpty(exe_path) )
      this.exePath = exe_path;
    else
      this.exePath = findCalibreDb();

    final ImmutableList.Builder<String> bldr = ImmutableList.builder();

    if ( Str.isNotEmpty(library_path) )
      bldr.add("--library-path=" + library_path);

    if ( other_options != null )
      bldr.addAll(other_options);

    this.options = bldr.build();
  }

  /**
   * Adds a document to a Calibre e&ndash;book library.
   * 
   * @param file
   *        The document file to add.
   * @param title
   *        An optional title (use an empty string or <code>null</code> for
   *        unspecified or to use whatever is in the <code>file</code>.) See
   *        <code><a href="http://manual.calibre-ebook.com/cli/calibredb.html#cmdoption-calibredb-add--title">--title</a></code>
   *        in the Calibre documentation.
   * @param author
   *        An optional author (use an empty string or <code>null</code> for
   *        unspecified or to use whatever is in the <code>file</code>.) See
   *        <code><a href="http://manual.calibre-ebook.com/cli/calibredb.html#cmdoption-calibredb-add--authors">--authors</a></code>
   *        in the Calibre documentation.
   * @throws IOException
   */
  public void add( final File file, final String title, final String author )
    throws IOException
  {
    final List<String> cmd = Colls.newArrayList(4 + options.size());
    cmd.add(exePath);
    cmd.add("add");
    cmd.addAll(options);
    if ( Str.isNotEmpty(title) )
      cmd.add("--title=" + title);
    if ( Str.isNotEmpty(author) )
      cmd.add("--authors=" + author);
    cmd.add(file.toString());

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
      log.trace("calibredb add returned {}", ret);

    log.trace("calibredb add output:");
    for ( String line : output )
      log.trace(line);

    boolean okay = ret == 0;
    if ( okay )
      for ( String line : output )
        if ( line.endsWith(" not found") )
        {
          okay = false;
          break;
        }

    if ( !okay )
      throw new IOException("Error adding file " + file.toString()
          + " to Calibre library. See log file for details.");
  }
}
