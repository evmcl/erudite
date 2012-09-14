package com.evanmclean.erudite.misc;

import java.io.File;

import com.evanmclean.evlib.lang.Str;

/**
 * Some functions for dealing with file names.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class FileName
{
  /**
   * <p>
   * Get the base name, without any leading path elements.
   * </p>
   * 
   * <p>
   * Examples:
   * </p>
   * 
   * <pre>
   *   /fred.home/file.txt => "file.txt"
   *   /fred.home/         => "fred.home"
   * </pre>
   * 
   * @param orig
   * @return The base name.
   */
  public static String baseName( final String orig )
  {
    final int len = endPos(orig);
    int pos = len;
    while ( (pos > 0) && (!isDirSep(orig.charAt(pos - 1))) )
      --pos;
    return orig.substring(pos, len);
  }

  /**
   * <p>
   * Get the extension for a file.
   * </p>
   * 
   * <p>
   * Examples:
   * </p>
   * 
   * <pre>
   *   /fred.home/file.txt => "txt"
   *   /fred.home/file     => ""
   *   /fred.home/.file    => ""
   *   /fred.home/         => "home"
   * </pre>
   * 
   * @param orig
   *        The file to return the extension.
   * @return The extension for the file.
   */
  public static String extension( final File orig )
  {
    return extension(orig.getPath());
  }

  /**
   * <p>
   * Get the extension for a file name.
   * </p>
   * 
   * <p>
   * Examples:
   * </p>
   * 
   * <pre>
   *   /fred.home/file.txt => "txt"
   *   /fred.home/file     => ""
   *   /fred.home/.file    => ""
   *   /fred.home/         => "home"
   *   /                   => ""
   * </pre>
   * 
   * @param orig
   *        The file name to return the extension.
   * @return The extension for the file.
   */
  public static String extension( final String orig )
  {
    final int len = endPos(orig);
    final int dot = extensionPos(orig);
    if ( (dot + 1) > len )
      return Str.EMPTY;
    return orig.substring(dot + 1, len);
  }

  /**
   * <p>
   * Get the file name without the extension.
   * </p>
   * 
   * <p>
   * Examples:
   * </p>
   * 
   * <pre>
   *   /fred.home/file.txt => "/fred.home/file"
   *   /fred.home/file     => "/fred.home/file"
   *   /fred.home/.file    => "/fred.home/.file"
   *   /fred.home/         => "/fred"
   *   /                   => "/"
   * </pre>
   * 
   * @param orig
   *        The file name to return without the extension.
   * @return The file name without the extension.
   */
  public static String sansExtension( final File orig )
  {
    return sansExtension(orig.getPath());
  }

  /**
   * <p>
   * Get the file name without the extension.
   * </p>
   * 
   * <p>
   * Examples:
   * </p>
   * 
   * <pre>
   *   /fred.home/file.txt => "/fred.home/file"
   *   /fred.home/file     => "/fred.home/file"
   *   /fred.home/.file    => "/fred.home/.file"
   *   /fred.home/         => "/fred"
   *   /                   => "/"
   * </pre>
   * 
   * @param orig
   *        The file name to return without the extension.
   * @return The file name without the extension.
   */
  public static String sansExtension( final String orig )
  {
    if ( Str.isEmpty(orig) )
      return Str.EMPTY;
    return orig.substring(0, extensionPos(orig));
  }

  /**
   * Finds the end of the file name without any trailing directory seperators.
   * 
   * @param filename
   * @return
   */
  private static int endPos( final String filename )
  {
    final int olen = Str.length(filename);
    int len = olen;
    while ( (len > 0) && isDirSep(filename.charAt(len - 1)) )
      --len;
    if ( (len <= 0) && (olen > 0) && isDirSep(filename.charAt(0)) )
      return 1;
    return len;
  }

  private static int extensionPos( final String filename )
  {
    final int len = endPos(filename);
    for ( int pos = len - 1; pos > 0; --pos )
    {
      final char ch = filename.charAt(pos);
      if ( ch == '.' )
      {
        // Ensure file name does not start with a dot.
        if ( isDirSep(filename.charAt(pos - 1)) )
          return len;
        return pos;
      }
      if ( isDirSep(ch) )
        break;
    }
    return len;
  }

  private static boolean isDirSep( final char ch )
  {
    return (ch == '/') || (ch == '\\');
  }

  private FileName()
  {
    // empty
  }
}
