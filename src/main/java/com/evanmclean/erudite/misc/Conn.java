package com.evanmclean.erudite.misc;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

/**
 * <p>
 * Creates a HTTP {@link Connection}, configured for the application.
 * </p>
 * 
 * <p>
 * Basically sets the user agent to &ldquo;erudite&rdquo; and tweaks other
 * parameters of the connection such as the timeout.
 * </p>
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class Conn
{
  private static final int TIMEOUT = 30000;

  /**
   * Return a HTTP connection for a URL.
   * 
   * @param url
   *        The URL to connect to.
   * @return A new {@link Connection}.
   */
  public static Connection connect( final String url )
  {
    final Connection conn = Jsoup.connect(url);
    conn.userAgent("erudite");
    conn.timeout(TIMEOUT);
    return conn;
  }

  private Conn()
  {
    // empty
  }
}
