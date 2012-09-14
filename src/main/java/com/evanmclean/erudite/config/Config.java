package com.evanmclean.erudite.config;

import com.google.common.collect.ImmutableList;

/**
 * <p>
 * Represents the configuration for the application as a set of key value pairs
 * (usually from a file).
 * </p>
 * <p>
 * Configuration files have the following properties.
 * </p>
 * <ul>
 * <li>Keys with no value (i.e., an empty string) are treated the same as if the
 * key was not specified at all.</li>
 * <li>All leading and trailing white space is removed from a value before being
 * returned (or converted).</li>
 * <li>A key may be specified multiple times, but with the exception of
 * {@link #getStrings(String)} the configuration will only use the last
 * (non-empty) value specified.</li>
 * </ul>
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface Config
{
  /**
   * Retrieve a boolean configuration variable. One of (case insenstive) &ldquo;
   * <code>true</code> &rdquo;, &ldquo;<code>yes</code>&rdquo;, &ldquo;
   * <code>y</code>&rdquo; or &ldquo;<code>1</code>&rdquo; for true, or &ldquo;
   * <code>false</code>&rdquo;, &ldquo;<code>no</code>&rdquo;, &ldquo;
   * <code>n</code>&rdquo; or &ldquo; <code>0</code>&rdquo; for false.
   * 
   * @param key
   *        The key to look-up.
   * @param def
   *        What to return if the key is unspecified or empty.
   * @return The value or the default.
   * @throws IllegalStateException
   *         Thrown if the value is not one of the above valid values.
   */
  boolean getBoolean( String key, boolean def );

  /**
   * Retrieve a integer configuration variable.
   * 
   * @param key
   *        The key to look-up.
   * @param def
   *        What to return if the key is unspecified or empty.
   * @return The value or the default.
   * @throws IllegalStateException
   *         Thrown if the value is not one of the above valid values.
   */
  int getInt( String key, int def );

  /**
   * Return a string configuration variable. Equivalent to
   * <code>get(key, null)</code>.
   * 
   * @param key
   *        The key to look-up.
   * @return The value, or <code>null</code> if the key is unspecified or empty.
   */
  String getString( String key );

  /**
   * Return a string configuration variable.
   * 
   * @param key
   *        The key to look-up.
   * @param def
   *        What to return if the key is unspecified or empty (may be
   *        <code>null</code>).
   * @return The value or the default.
   */
  String getString( String key, String def );

  /**
   * An array of strings from a key that is specified zero or more times within
   * the configuration file.
   * 
   * @param key
   *        The key to look-up.
   * @return A list of the values (will be an empty list if the key is
   *         unspecified, empty values are ignored).
   */
  ImmutableList<String> getStrings( String key );

  /**
   * The title munger, built from the values of
   * <code>getStrings(&quot;title&quot;)</code>. See the sample configuration
   * file in the user documentation.
   * 
   * @return The title munger.
   */
  TitleMunger getTitleMunger();
}
