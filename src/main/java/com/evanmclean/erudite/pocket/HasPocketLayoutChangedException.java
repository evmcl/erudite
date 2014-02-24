package com.evanmclean.erudite.pocket;

/**
 * Thrown if our page scraping of the Instapaper web-site files for some reason.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class HasPocketLayoutChangedException extends RuntimeException
{
  HasPocketLayoutChangedException( final String message )
  {
    super(message);
  }

  HasPocketLayoutChangedException( final String message,
      final Throwable cause )
  {
    super(message, cause);
  }

  HasPocketLayoutChangedException( final Throwable cause )
  {
    super(cause);
  }
}
