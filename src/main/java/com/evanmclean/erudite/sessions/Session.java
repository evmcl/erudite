package com.evanmclean.erudite.sessions;

import java.io.IOException;
import java.io.Serializable;

import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.instapaper.Instapaper;

/**
 * <p>
 * A serialisable object that contains the cookies, API key or whatever needed
 * to access a {@link Source} of articles.
 * </p>
 * 
 * <p>
 * For example, call the static method {@link Instapaper#login(String, String)}
 * to create a session to access Instapaper.
 * </p>
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public interface Session extends Serializable
{
  /**
   * Create a {@link Source} using the access details from this session and the
   * specified configuration.
   * 
   * @param config
   *        The configuration info for the source.
   * @return A source object for retrieving articles.
   * @throws IOException
   */
  Source getSource( Config config ) throws IOException;

  /**
   * The type of source this session accesses.
   * 
   * @return The type of source this session accesses.
   */
  SourceType getSourceType();
}
