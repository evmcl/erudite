package com.evanmclean.erudite.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;

import com.evanmclean.evlib.charset.Charsets;
import com.evanmclean.evlib.lang.Str;

/**
 * <p>
 * A hack for allowing better wrapping of URLs in an epub file by injecting <a
 * href="http://en.wikipedia.org/wiki/Zero_width_space">zero width spaces</a>.
 * </p>
 * 
 * <p>
 * Calibre's <a href="http://manual.calibre-ebook.com/cli/ebook-convert.html">
 * <code>ebook-convert</code></a> program filters out zero width spaces when
 * converting HTML files to epub, so instead we inject a unique string in all
 * the places we want to allow long URLs to word-wrap, then afterwards go
 * through the contents of the epub file produced and swap it for our special
 * space character.
 * </p>
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class EpubWrapHack
{
  private static final String MAGIC = "@@ERUDITE@@WRAP@@HACK@@";
  private static final byte[] MAGIC_BYTES = MAGIC.getBytes(Charsets.UTF8);
  private static final byte[] ZERO_WIDTH_BYTES = "\u200B"
      .getBytes(Charsets.UTF8);

  /**
   * Process the produced epub file, swapping our special marker for zero width
   * spaces.
   * 
   * @param from
   *        epub file containing our special markers.
   * @param to
   *        epub file to be written with zero width spaces inserted.
   * @throws IOException
   */
  public static void postProcess( final File from, final File to )
    throws IOException
  {
    final InputStream in = new FileInputStream(from);
    try
    {
      final OutputStream out = new FileOutputStream(to);
      try
      {
        postProcess(in, out);
      }
      finally
      {
        out.close();
      }
    }
    finally
    {
      in.close();
    }
  }

  /**
   * Pre-process our HTML document, looking for elements with the class &ldquo;
   * <code>erudite_url_url</code>&rdquo; and inserting our special marker in the
   * contained text at the points were we want to allow word wrapping.
   * 
   * @param orig
   *        The document to process.
   * @return A copy of <code>orig</code> with our markers inserted.
   */
  public static Document preProcess( final Document orig )
  {
    LoggerFactory.getLogger(EpubWrapHack.class).trace(
      "Performing Epub Wrap Hack pre-processing.");
    final Document doc = orig.clone();
    for ( Element el : doc.getElementsByClass("erudite_url_url") )
      hack(el);
    return doc;
  }

  private static void copy( final InputStream in, final OutputStream out,
      final byte[] buff ) throws IOException
  {
    int len;
    while ( (len = in.read(buff)) >= 0 )
      out.write(buff, 0, len);
  }

  private static int findMagic( final byte[] buff, final int start,
      final int end )
  {
    final int max = end - MAGIC_BYTES.length + 1;
    outer: for ( int xi = start; xi < max; ++xi )
    {
      for ( int xj = 0; xj < MAGIC_BYTES.length; ++xj )
        if ( buff[xi + xj] != MAGIC_BYTES[xj] )
          continue outer;
      return xi;
    }
    return -1;
  }

  private static boolean hack( final Element element )
  {
    final StringBuilder text = new StringBuilder(element.text());
    boolean changed = false;
    for ( int xi = text.length() - 3; xi > 10; --xi )
      switch ( text.charAt(xi) )
      {
        case '/':
        case '=':
        case '&':
        case '?':
          text.insert(xi, MAGIC);
          changed = true;
          break;
      }
    if ( changed )
      element.text(text.toString());
    return changed;
  }

  private static void postProcess( final InputStream from, final OutputStream to )
    throws IOException
  {
    final ZipInputStream in = new ZipInputStream(from);
    final ZipOutputStream out = new ZipOutputStream(to);
    try
    {
      out.setLevel(9);
      postProcess(in, out);
    }
    finally
    {
      out.finish();
    }
  }

  private static void postProcess( final ZipInputStream from,
      final ZipOutputStream to ) throws IOException
  {
    LoggerFactory.getLogger(EpubWrapHack.class).trace(
      "Performing Epub Wrap Hack post-processing.");
    final byte[] buff = new byte[1024 * 1024];
    ZipEntry inentry;
    while ( (inentry = from.getNextEntry()) != null )
    {
      final boolean is_html = Str
          .endsWithIgnoreCase(inentry.getName(), ".html");
      final ZipEntry outentry = new ZipEntry(inentry.getName());
      to.putNextEntry(outentry);
      try
      {
        if ( is_html )
          unhack(from, to, buff);
        else
          copy(from, to, buff);
      }
      finally
      {
        to.closeEntry();
      }
    }
  }

  private static void unhack( final InputStream in, final OutputStream out,
      final byte[] buff ) throws IOException
  {
    int pos = 0;
    boolean eof = false;
    while ( !eof )
    {
      final int cnt = in.read(buff, pos, buff.length - pos);
      eof = cnt < 0;
      final int end = (cnt <= 0) ? pos : pos + cnt;
      final int lastpos = (cnt < 0) ? end //
          : Math.max(0, end - MAGIC_BYTES.length + 1);
      pos = 0;
      while ( pos < lastpos )
      {
        int idx = findMagic(buff, pos, end);
        if ( idx < 0 )
        {
          if ( pos < lastpos )
          {
            out.write(buff, pos, lastpos - pos);
            pos = lastpos;
          }
        }
        else
        {
          if ( pos < idx )
            out.write(buff, pos, idx - pos);
          out.write(ZERO_WIDTH_BYTES);
          pos = idx + MAGIC_BYTES.length;
        }
      }

      if ( pos >= end )
      {
        pos = 0;
      }
      else
      {
        final int len = end - pos;
        System.arraycopy(buff, pos, buff, 0, len);
        pos = len;
      }
    }
  }

  private EpubWrapHack()
  {
    // empty
  }
}
