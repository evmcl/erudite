package com.evanmclean.erudite.misc;

import java.io.File;
import java.io.IOException;

import com.evanmclean.evlib.io.Folders;

/**
 * Create a new file with a unique file name.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class UniqueFile
{
  private static boolean fileOkay( final File file, final File lfile )
    throws IOException
  {
    if ( file.exists() || lfile.exists() )
      return false;
    return file.createNewFile();
  }

  private final String baseName;
  private final File file;

  /**
   * Create a new file.
   * 
   * @param base_folder
   *        The folder in which to create the file.
   * @param prefix
   *        The start of the file name.
   * @param suffix
   *        The end of the file name (e.g., &ldquo;<code>.txt</code>&rdquo;).
   * @throws IOException
   */
  public UniqueFile( final File base_folder, final String prefix,
      final String suffix ) throws IOException
  {
    File afile = new File(base_folder, prefix + suffix);
    File lfile = new File(base_folder, prefix.toLowerCase() + suffix);
    String name = prefix;

    Folders.mks(base_folder);

    if ( !fileOkay(afile, lfile) )
    {
      int num = 0;
      while ( !fileOkay(afile, lfile) )
      {
        ++num;
        final StringBuilder buff = new StringBuilder(prefix);
        buff.append('_');
        if ( num < 10 )
          buff.append("00");
        else if ( num < 100 )
          buff.append('0');
        buff.append(num);
        name = buff.toString();
        afile = new File(base_folder, name + suffix);
        lfile = new File(base_folder, name.toLowerCase() + suffix);
      }
    }

    this.baseName = name;
    this.file = afile;
  }

  /**
   * The base name that the file ended up having (sans the suffix).
   * 
   * @return The base name of the file (without the path or suffix).
   */
  public String getBaseName()
  {
    return baseName;
  }

  /**
   * The file that was created.
   * 
   * @return The file that was created.
   */
  public File getFile()
  {
    return file;
  }
}
