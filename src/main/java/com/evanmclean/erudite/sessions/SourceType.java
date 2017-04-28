package com.evanmclean.erudite.sessions;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Source;

/**
 * The various {@link Source}s of {@link Article}s this program can utilise.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public enum SourceType
{
  /**
   * <a href="https://www.instapaper.com/">Instapaper</a>
   */
  INSTAPAPER //
  /**
   * <a href="https://getpocket.com/">Pocket</a>
   */
  , POCKET //
  ;

  /**
   * Get source where <code>str</code> matches that start of the source name
   * (case-insensitive).
   * 
   * @param str
   * @return The matching source.
   * @throws IllegalArgumentException
   *         if <code>str</code> is <code>null</code> or does not match an
   *         source.
   */
  public static SourceType get( final String str )
  {
    if ( str != null )
    {
      SourceType match = null;
      final int len = str.length();
      for ( final SourceType source : SourceType.values() )
      {
        final String ssource = source.name();
        final int slen = ssource.length();
        if ( len < slen )
        {
          if ( str.equalsIgnoreCase(ssource.substring(0, len)) )
          {
            if ( match != null ) // More than one match.
              throw new IllegalArgumentException("No such source: " + str);
            match = source;
          }
        }
        else if ( len == slen )
        {
          if ( str.equalsIgnoreCase(ssource) )
          {
            if ( match != null ) // More than one match.
              throw new IllegalArgumentException("No such source: " + str);
            match = source;
          }
        }
      }
      if ( match != null )
        return match;
    }
    throw new IllegalArgumentException("No such source: " + str);
  }
}
