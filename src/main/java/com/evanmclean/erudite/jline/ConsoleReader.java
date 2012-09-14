package com.evanmclean.erudite.jline;

import java.io.IOException;

import com.evanmclean.evlib.lang.Str;

/**
 * A console reader that has some special code for dealing with a cygwin
 * environment.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class ConsoleReader extends jline.console.ConsoleReader
{
  public static final boolean IS_CYGWIN_XTERM = isCygwinXterm();

  static
  {
    if ( IS_CYGWIN_XTERM )
      if ( Str.isEmpty(System.getProperty("jline.terminal")) )
        System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
  }

  private static boolean isCygwinXterm()
  {
    final String os = Str.ifNull(System.getProperty("os.name")).toLowerCase();
    if ( os.indexOf("windows") < 0 )
      return false;
    final String term = Str.ifNull(System.getenv("TERM"));
    return term.equals("xterm");
  }

  public ConsoleReader() throws IOException
  {
    super();
  }
}
