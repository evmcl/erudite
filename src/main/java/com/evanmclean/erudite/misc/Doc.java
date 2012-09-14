package com.evanmclean.erudite.misc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.jsoup.nodes.Document;

import com.evanmclean.evlib.charset.Charsets;
import com.evanmclean.evlib.io.UTF8FileWriter;

/**
 * Some functions for writing HTML {@link Document}s.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class Doc
{
  public static void write( final Document doc, final File file )
    throws IOException
  {
    final Writer out = new UTF8FileWriter(file);
    try
    {
      write(doc, out);
    }
    finally
    {
      out.close();
    }
  }

  public static void write( final Document doc, final OutputStream out )
    throws IOException
  {
    final Writer wout = new OutputStreamWriter(out, Charsets.UTF8);
    write(doc, wout);
    wout.flush();
  }

  public static void write( final Document doc, final Writer out )
    throws IOException
  {
    out.write(doc.outerHtml());
  }

  private Doc()
  {
    // empty
  }
}
