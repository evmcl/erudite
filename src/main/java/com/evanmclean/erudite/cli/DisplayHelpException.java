package com.evanmclean.erudite.cli;

/**
 * Thrown by {@link Args} constructor if the command-line usage should be
 * displayed.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class DisplayHelpException extends Exception
{
  DisplayHelpException()
  {
    // empty
  }

  DisplayHelpException( final Throwable cause )
  {
    super(cause.getMessage(), cause);
  }
}
