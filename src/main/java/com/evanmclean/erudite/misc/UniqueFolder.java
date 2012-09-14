package com.evanmclean.erudite.misc;

import java.io.File;
import java.io.IOException;

import com.evanmclean.evlib.io.Folders;

/**
 * Create a new folder with a unique name.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class UniqueFolder
{
  private static boolean folderOkay( final File folder, final File lfolder )
  {
    if ( folder.exists() || lfolder.exists() )
      return false;
    return folder.mkdir();
  }

  private final String baseName;
  private final File folder;

  /**
   * Create a new folder.
   * 
   * @param base_folder
   *        The folder in which to create the sub-folder.
   * @param prefix
   *        The start of the folder name.
   * @param suffix
   *        The end of the folder name (can be an empty string.)
   * @throws IOException
   */
  public UniqueFolder( final File base_folder, final String prefix,
      final String suffix ) throws IOException
  {
    File afolder = new File(base_folder, prefix + suffix);
    File lfolder = new File(base_folder, prefix.toLowerCase() + suffix);
    String name = prefix;

    Folders.mks(base_folder);

    if ( !folderOkay(afolder, lfolder) )
    {
      int num = 0;
      while ( !folderOkay(afolder, lfolder) )
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
        afolder = new File(base_folder, name + suffix);
        lfolder = new File(base_folder, name.toLowerCase() + suffix);
      }
    }

    this.baseName = name;
    this.folder = afolder;
  }

  /**
   * The base name that the folder ended up having (sans the suffix).
   * 
   * @return The base name of the folder (without the path or suffix).
   */
  public String getBaseName()
  {
    return baseName;
  }

  /**
   * The folder that was created.
   * 
   * @return The folder that was created.
   */
  public File getFolder()
  {
    return folder;
  }
}
