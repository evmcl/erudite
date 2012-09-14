package com.evanmclean.erudite.config;

import com.evanmclean.evlib.lang.Str;

/**
 * Various utilities that can be of use when dealing with a {@link Config}.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class ConfigUtils
{
  /**
   * Return the value of the first key specified in the configuration.
   * 
   * @param config
   * @param key1
   * @param key2
   * @return The value of the first key specified in the configuration, or
   *         <code>null</code>.
   */
  public static String getFirst( final Config config, final String key1,
      final String key2 )
  {
    String ret = config.getString(key1);
    if ( ret == null )
      ret = config.getString(key2);
    return ret;
  }

  /**
   * Return the value of the first key specified in the configuration.
   * 
   * @param config
   * @param key1
   * @param key2
   * @param key3
   * @return The value of the first key specified in the configuration, or
   *         <code>null</code>.
   */
  public static String getFirst( final Config config, final String key1,
      final String key2, final String key3 )
  {
    String ret = getFirst(config, key1, key2);
    if ( ret == null )
      ret = config.getString(key3);
    return ret;
  }

  /**
   * Return the value of the first key specified in the configuration.
   * 
   * @param config
   * @param key1
   * @param key2
   * @param key3
   * @param key4
   * @return The value of the first key specified in the configuration, or
   *         <code>null</code>.
   */
  public static String getFirst( final Config config, final String key1,
      final String key2, final String key3, final String key4 )
  {
    String ret = getFirst(config, key1, key2, key3);
    if ( ret == null )
      ret = config.getString(key4);
    return ret;
  }

  /**
   * Convert a string to a boolean value. One of (case insenstive) &ldquo;
   * <code>true</code> &rdquo;, &ldquo;<code>yes</code>&rdquo;, &ldquo;
   * <code>y</code>&rdquo; or &ldquo;<code>1</code>&rdquo; for true, or &ldquo;
   * <code>false</code>&rdquo;, &ldquo;<code>no</code>&rdquo;, &ldquo;
   * <code>n</code>&rdquo; or &ldquo; <code>0</code>&rdquo; for false.
   * 
   * @param str
   *        The string to convert.
   * @param def
   *        What to return if the string is empty or <code>null</code>.
   * @return The value or the default.
   * @throws IllegalStateException
   *         Thrown if the value is not one of the above valid values.
   */
  public static boolean toBoolean( final String str, final boolean def )
  {
    if ( Str.isEmpty(str) )
      return def;
    if ( Str.equalsOneOfIgnoreCase(str, "true", "y", "1", "yes") )
      return true;
    if ( Str.equalsOneOfIgnoreCase(str, "false", "n", "0", "no") )
      return false;
    throw new IllegalStateException("Invalid boolean value: " + str);
  }

  private ConfigUtils()
  {
    // empty
  }
}
