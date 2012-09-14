package com.evanmclean.erudite.logback;

/**
 * The options for logging to the console or standard output.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public enum ConsoleLogging
{
  /**
   * Log all debug level and above messages.
   */
  VERBOSE,
  /**
   * Log all info level and above messages.
   */
  NORMAL,
  /**
   * Only write logs to the console if there was an error.
   */
  QUIET,
  /**
   * Nothing written to the console.
   */
  SILENT;
}
