package com.evanmclean.erudite.then;

import java.io.File;

import com.evanmclean.evlib.io.Files;
import com.evanmclean.evlib.io.Folders;

/**
 * A simple implementation of a {@link Reservation}.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class SimpleReservation implements Reservation
{
  private final File file;
  private final File folder;
  private final String baseName;
  private final String fileName;
  private final String folderName;

  public SimpleReservation( final File file, final File folder,
      final String base_name, final String file_name, final String folder_name )
  {
    this.file = file;
    this.folder = folder;
    this.baseName = base_name;
    this.fileName = file_name;
    this.folderName = folder_name;
  }

  @Override
  public void cleanup()
  {
    if ( file != null )
      Files.delhard(file);
    if ( folder != null )
      Folders.delQuietly(folder);
  }

  @Override
  public String getBaseName()
  {
    return baseName;
  }

  @Override
  public File getFile()
  {
    return file;
  }

  @Override
  public String getFileName()
  {
    return fileName;
  }

  @Override
  public File getFolder()
  {
    return folder;
  }

  @Override
  public String getFolderName()
  {
    return folderName;
  }

}
