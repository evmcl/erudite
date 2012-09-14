package com.evanmclean.erudite.instapaper;

/**
 * Thrown if our page scraping of the Instapaper web-site files for some reason.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class HasInstapaperLayoutChangedException extends RuntimeException
{
  HasInstapaperLayoutChangedException( final String message )
  {
    super(message);
  }

  HasInstapaperLayoutChangedException( final String message,
      final Throwable cause )
  {
    super(message, cause);
  }

  HasInstapaperLayoutChangedException( final Throwable cause )
  {
    super(cause);
  }
}
