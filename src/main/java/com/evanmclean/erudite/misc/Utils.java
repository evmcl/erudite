package com.evanmclean.erudite.misc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.nodes.Element;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Source;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.io.Files;
import com.evanmclean.evlib.lang.Str;
import com.google.common.collect.ImmutableList;

/**
 * Anything that don&rsquo;t fit elsewhere.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class Utils
{
  /**
   * Are we running on Windows?
   */
  public static final boolean IS_WINDOWS = Str
      .ifNull(System.getProperty("os.name")).toLowerCase().indexOf("windows") >= 0;

  private static String[] _paths;

  /**
   * If the string is too long, then abbreviate it to an excerpt with a trailing
   * ellipsis.
   * 
   * @param text
   *        String to abbreviate if necessary.
   * @param maxlen
   *        Maximum length of the returned string.
   * @return Abbreviated string.
   */
  public static String abbreviate( final String text, final int maxlen )
  {
    final String str = Str.trimToEmpty(text);
    if ( Str.length(str) <= maxlen )
      return str;

    // Find the end of a word.
    final int minlen = Math.max(1, maxlen / 2);
    int pos = maxlen - 1;
    while ( (pos >= minlen) && (!Character.isWhitespace(str.charAt(pos))) )
      --pos;
    while ( (pos >= minlen)
        && (!Character.isLetterOrDigit(str.charAt(pos - 1))) )
      --pos;
    if ( pos < minlen )
      pos = maxlen - 1;
    return str.substring(0, pos) + '\u2026';
  }

  /**
   * Find an existing file an array of paths.
   * 
   * @param name
   *        The name of the file we are looking for.
   * @param paths
   *        The paths to search.
   * @return The existing file in the first matching path, or <code>null</code>
   *         if not found.
   */
  public static File findInFolder( final String name, final String... paths )
  {
    for ( final String path : paths )
    {
      final File file = new File(path, name);
      if ( file.exists() )
        return Files.getCanonicalFile(file);
    }
    return null;
  }

  /**
   * Searches for an existing file in the folders specified by the operating
   * system's PATH environment variable.
   * 
   * @param name
   *        The name of the file we are looking for.
   * @return The existing file in the first matching path, or <code>null</code>
   *         if not found.
   */
  public static File findOnPath( final String name )
  {
    return findInFolder(name, getPaths());
  }

  /**
   * Create a HTML slug summary of the article. This basically looks for the
   * first paragraph tag, then starts converting each paragraph to text until it
   * gets about 1,000 characters.
   * 
   * @param source
   *        The source of the article.
   * @param article
   *        The article to summarise.
   * @param hacker_news_urls
   *        The hacker news discussion URLs for the article if available.
   * @return The HTML summary.
   */
  public static String summary( final Source source, final Article article,
      final ImmutableList<String> hacker_news_urls )
  {
    // IMPORTANT NOTE: Usage single quotes instead of double-quotes in the HTML
    // code you generate. When using the summary as a command line argument
    // under windows, the double-quotes tends to break things.

    final int MAXLEN = 1000;

    String summary = abbreviate(article.getSummary(), MAXLEN);
    if ( Str.isNotEmpty(summary) )
    {
      summary = "<p>" + Esc.htmlFull.text(summary) + "</p>";
    }
    else
    {
      try
      {
        Element el = null;
        // Find first paragraph element.
        {
          Iterator<Element> it = article.text().children().iterator();
          OUTER: while ( it.hasNext() )
          {
            final Element top = it.next();
            if ( top.tagName().equalsIgnoreCase("p") )
            {
              if ( top.hasText() )
              {
                el = top;
                break;
              }
            }
            else
            {
              for ( Element sub : top.getElementsByTag("p") )
                if ( (sub != null) && sub.hasText() )
                {
                  el = sub;
                  break OUTER;
                }
            }
          }
        }

        // Now run through current and following elements to build up the
        // summary text.
        final StringBuilder buff = new StringBuilder(MAXLEN + 100);
        int lentogo = MAXLEN;
        final int MINLEN = 100;
        while ( (el != null) && (lentogo > MINLEN) )
        {
          if ( el.hasText() )
          {
            final String text = Utils.abbreviate(el.text(), lentogo);
            buff.append("<p>").append(Esc.htmlFull.text(text)).append("</p>");
            lentogo -= text.length();
            if ( lentogo <= MINLEN )
              break;
          }

          Element next = el.nextElementSibling();
          while ( next == null )
            try
            {
              if ( el != null )
                el = el.parent();
              if ( (el == null) || (el.parent() == null) )
                break;
              next = el.nextElementSibling();
            }
            catch ( NullPointerException ex )
            {
              // Sometimes we get an unexpected null point exception from the
              // jsoup library, in which case we just run with what we have.
              next = null;
            }
          el = next;
        }
        summary = buff.toString();
      }
      catch ( IOException ex )
      {
        summary = Str.EMPTY;
      }
    }

    // IMPORTANT NOTE: Usage single quotes instead of double-quotes in the HTML
    // code you generate. When using the summary as a command line argument
    // under windows, the double-quotes tends to break things.

    final String original_url = article.getOriginalUrl();
    final String source_url = article.getSourceUrl();
    final StringBuilder buff = new StringBuilder();

    buff.append("<p><a href='").append(Esc.htmlFull.attr(original_url))
        .append("'>").append(Esc.htmlFull.text(original_url)).append("</a>");

    if ( Str.isNotEmpty(source_url) )
      buff.append("<span style='font-size: 80%'><br><a href='")
          .append(Esc.htmlFull.attr(source_url)).append("'>Read at ")
          .append(Esc.htmlFull.text(source.getName())).append(".</a></span>");

    if ( (hacker_news_urls != null) && (!hacker_news_urls.isEmpty()) )
    {
      buff.append("<span style='font-size: 80%'><br>(");
      if ( hacker_news_urls.size() == 1 )
      {
        buff.append("<a href='")
            .append(Esc.htmlFull.attr(hacker_news_urls.get(0)))
            .append("'>Hacker News discussion.</a>");
      }
      else
      {
        buff.append("Hacker News discussions: ");
        int link_num = 0;
        for ( final String hn_url : hacker_news_urls )
        {
          if ( link_num > 0 )
            buff.append(", ");
          buff.append(" <a href='").append(Esc.htmlFull.attr(hn_url))
              .append("'>#").append(++link_num).append("</a>");
        }
        buff.append('.');
      }
      buff.append(")</span>");
    }

    buff.append("</p>");
    if ( Str.isNotEmpty(summary) )
      buff.append(summary);
    return buff.toString();
  }

  private static String[] getPaths()
  {
    String[] paths = _paths;
    if ( paths == null )
    {
      final String pathstr = System.getenv("PATH");
      if ( Str.isEmpty(pathstr) )
      {
        paths = new String[0];
      }
      else
      {
        final String[] arr = pathstr.split(IS_WINDOWS ? ";" : ":");
        final ArrayList<String> list = new ArrayList<String>(arr.length);
        for ( String str : arr )
          if ( Str.isNotEmpty(str) )
            list.add(str);
        paths = list.toArray(new String[list.size()]);
      }
      _paths = paths;
    }
    return paths;
  }

  private Utils()
  {
    // empty
  }
}
