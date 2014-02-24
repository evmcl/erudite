package com.evanmclean.erudite.then;

import com.evanmclean.erudite.config.Config;
import com.evanmclean.evlib.lang.Str;

/**
 * Produce {@link Then} hander objects based on a configuration file.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class ThenFactory
{
  /**
   * Create the specified {@link Then} handler.
   * 
   * @param prefix
   *        The prefix of the keys to read in the configuration.
   * @param config
   *        The configuration to read.
   * @return The handler object.
   */
  public static Then get( final String prefix, final Config config )
  {
    final String str = config.getString(prefix + "then");

    if ( Str.equalsIgnoreCase(str, "calibre") )
      return new CalibreThen(prefix, config);
    if ( Str.equalsIgnoreCase(str, "save") )
      return new SaveThen(prefix, config);

    if ( Str.isEmpty(str) )
      throw new IllegalStateException(prefix + "then action not specified.");
    throw new IllegalStateException("Unknown " + prefix + "then action: " + str);
  }

  private ThenFactory()
  {
    // empty
  }
}
