package com.evanmclean.erudite.misc;

import java.io.File;
import java.io.IOException;

import com.evanmclean.evlib.io.Files;
import com.evanmclean.evlib.io.Folders;

/**
 * Create a new file and folder based on a unique name.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class UniqueFileAndFolder
{
  private static boolean fileAndFolderOkay( final File file, final File lfile,
      final File folder, final File lfolder ) throws IOException
  {
    if ( file.exists() || folder.exists() || lfile.exists() || lfolder.exists() )
      return false;
    if ( !file.createNewFile() )
      return false;
    boolean okay = false;
    try
    {
      if ( folder.mkdir() )
        okay = true;
    }
    finally
    {
      if ( !okay )
        Files.delhard(file);
    }
    return okay;
  }

  private final String baseName;
  private final File file;
  private final File folder;

  /**
   * Create a new file and folder.
   * 
   * @param base_folder
   *        The folder in which to create the file and sub-folder.
   * @param prefix
   *        The start of the file and folder name.
   * @param file_suffix
   *        The end of the file name (e.g., &ldquo;<code>.txt</code>&rdquo;).
   * @param folder_suffix
   *        The end of the folder name (can be an empty string.)
   * @throws IOException
   */
  public UniqueFileAndFolder( final File base_folder, final String prefix,
      final String file_suffix, final String folder_suffix ) throws IOException
  {
    File afile = new File(base_folder, prefix + file_suffix);
    File lfile = new File(base_folder, prefix.toLowerCase() + file_suffix);
    File afolder = new File(base_folder, prefix + folder_suffix);
    File lfolder = new File(base_folder, prefix.toLowerCase() + folder_suffix);
    String name = prefix;

    Folders.mks(base_folder);

    if ( !fileAndFolderOkay(afile, lfile, afolder, lfolder) )
    {
      int num = 0;
      while ( !fileAndFolderOkay(afile, lfile, afolder, lfolder) )
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
        afile = new File(base_folder, name + file_suffix);
        lfile = new File(base_folder, name.toLowerCase() + file_suffix);
        afolder = new File(base_folder, name + folder_suffix);
        lfolder = new File(base_folder, name.toLowerCase() + folder_suffix);
      }
    }

    this.baseName = name;
    this.file = afile;
    this.folder = afolder;
  }

  /**
   * The base name that the file and folder ended up having (sans the suffixes).
   * 
   * @return The base name for the file and folder (without the path or
   *         suffixes).
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
