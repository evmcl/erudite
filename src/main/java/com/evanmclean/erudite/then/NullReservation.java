package com.evanmclean.erudite.then;

import java.io.File;

/**
 * A {@link Reservation} that does not actually reserve anything. Can be used by
 * a {@link Then} handler that does not actually need to reserve anything.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class NullReservation implements Reservation
{
  /**
   * An instance of this stateless object.
   */
  public static final NullReservation INSTANCE = new NullReservation();

  @Override
  public void cleanup()
  {
    // empty
  }

  @Override
  public String getBaseName()
  {
    return "article";
  }

  @Override
  public File getFile()
  {
    return null;
  }

  @Override
  public String getFileName()
  {
    return "article.deleteme";
  }

  @Override
  public File getFolder()
  {
    return null;
  }

  @Override
  public String getFolderName()
  {
    return "article_files";
  }
}
