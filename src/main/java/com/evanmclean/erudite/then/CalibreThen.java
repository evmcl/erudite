package com.evanmclean.erudite.then;

import java.io.File;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.calibre.CalibreAdder;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.config.ConfigUtils;

/**
 * Post processor that will add the produced document file(s) to a Calibre
 * e&ndash;book library. (Uses a {@link CalibreAdder} to do the dirty work.)
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class CalibreThen implements Then
{
  private final CalibreAdder ca;

  /**
   * Create the handler based on the configuration info.
   * 
   * @param prefix
   *        The prefix of the keys to read in the configuration.
   * @param config
   *        The configuration to read.
   */
  public CalibreThen( final String prefix, final Config config )
  {
    this.ca = new CalibreAdder( //
        ConfigUtils.getFirst(config, "calibredb.prog", "calibredb") //
        , ConfigUtils.getFirst(config, prefix + "library", "calibredb.library") //
        , config.getStrings(prefix + "then.calibredb.option") //
    );
  }

  @Override
  public Reservation reserve( final Article article, final String file_suffix,
      final String folder_suffix )
  {
    return NullReservation.INSTANCE;
  }

  @Override
  public Reservation reserve( final String base_name, final String file_suffix,
      final String folder_suffix )
  {
    return NullReservation.INSTANCE;
  }

  @Override
  public void then( final Reservation reservation, final File file,
      final File folder ) throws Exception
  {
    ca.add(file, null, null);
  }
}
