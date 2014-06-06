package com.evanmclean.erudite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.evanmclean.evlib.lang.Str;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

/**
 * A template encapsulates an HTML document that {@link Erudite} uses to produce
 * the nice, readable version of an article, complete with footnotes and such.
 *
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class Template
{
  private final Document doc;

  public Template( final Document doc )
  {
    this.doc = doc.clone();
  }

  public Template( final File file ) throws IOException
  {
    this(Files.asByteSource(file));
  }

  public Template( final ByteSource ins ) throws IOException
  {
    final InputStream in = ins.openStream();
    try
    {
      this.doc = Jsoup.parse(in, "UTF-8", Str.EMPTY);
    }
    finally
    {
      in.close();
    }
  }

  /**
   * Retrieves a copy of the HTML document. The calling code can modify this
   * without effecting the original template.
   *
   * @return A copy of the HTML document for this template.
   */
  public Document getDocument()
  {
    return doc.clone();
  }
}
