package com.evanmclean.erudite.then;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.config.ConfigUtils;
import com.evanmclean.erudite.misc.UniqueFile;
import com.evanmclean.erudite.misc.UniqueFileAndFolder;
import com.evanmclean.erudite.misc.UniqueFolder;
import com.evanmclean.evlib.io.Files;
import com.evanmclean.evlib.io.Folders;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.stringtransform.FilenameTransformer;

/**
 * Post processor that will save the produced document file(s) to a folder.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class SaveThen implements Then
{
  private final File baseFolder;

  /**
   * Create a handler that will save documents to a specified folder.
   * 
   * @param base_folder
   *        The folder to save documents.
   */
  public SaveThen( final File base_folder )
  {
    this.baseFolder = Files.getCanonicalFile(base_folder);
  }

  /**
   * Create the handler based on the configuration info.
   * 
   * @param prefix
   *        The prefix of the keys to read in the configuration.
   * @param config
   *        The configuration to read.
   */
  public SaveThen( final String prefix, final Config config )
  {
    final String str = ConfigUtils
        .getFirst(config, prefix + "saveto", "saveto");
    if ( Str.isEmpty(str) )
      throw new IllegalStateException("No saveto folder specified.");
    this.baseFolder = Files.getCanonicalFile(new File(str));
  }

  @Override
  public Reservation reserve( final Article article, final String file_suffix,
      final String folder_suffix ) throws IOException
  {
    return reserve(
      new FilenameTransformer(200).transform(article.getTitle(), "document"),
      file_suffix, folder_suffix);
  }

  @Override
  public Reservation reserve( final String base_name, final String file_suffix,
      final String folder_suffix ) throws IOException
  {
    if ( file_suffix != null )
    {
      if ( folder_suffix == null )
      {
        // Get unique file
        final UniqueFile uf = new UniqueFile(baseFolder, base_name, file_suffix);
        return new SimpleReservation(uf.getFile(), null, uf.getBaseName(), uf
            .getFile().getName(), null);
      }

      // Get unique file and folder
      final UniqueFileAndFolder uff = new UniqueFileAndFolder(baseFolder,
          base_name, file_suffix, folder_suffix);
      return new SimpleReservation(uff.getFile(), uff.getFolder(),
          uff.getBaseName(), uff.getFile().getName(), uff.getFolder().getName());
    }

    // Get unique folder
    final UniqueFolder uf = new UniqueFolder(baseFolder, base_name,
        folder_suffix);
    return new SimpleReservation(null, uf.getFolder(), uf.getBaseName(), null,
        uf.getFolder().getName());
  }

  @Override
  public void then( final Reservation reservation, final File from_file,
      final File from_folder ) throws IOException
  {
    if ( from_file == null )
    {
      if ( reservation.getFile() != null )
        Files.delhard(reservation.getFile());
    }
    else
    {
      if ( reservation.getFile() == null )
        throw new IOException("No target file allocated.");
      LoggerFactory.getLogger(SaveThen.class).debug("Saving to {}",
        reservation.getFile());
      Files.move(from_file, reservation.getFile());
    }

    if ( (from_folder == null) || Folders.isEmpty(from_folder) )
    {
      if ( reservation.getFolder() != null )
        Folders.delQuietly(reservation.getFolder());
    }
    else
    {
      if ( reservation.getFolder() == null )
        throw new IOException("No target folder allocated.");
      Folders.copy(from_folder, reservation.getFolder());
    }
  }
}
