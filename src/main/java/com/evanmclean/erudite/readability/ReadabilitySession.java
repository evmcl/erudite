package com.evanmclean.erudite.readability;

import org.scribe.model.Token;

import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.erudite.sessions.SourceType;

/**
 * Session object for Readability.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class ReadabilitySession implements Session
{
  private static final long serialVersionUID = -8577131670126950424L;

  private final String key;
  private final String secret;
  private final Token token;

  ReadabilitySession( final String key, final String secret, final Token token )
  {
    this.key = key;
    this.secret = secret;
    this.token = token;
  }

  @Override
  public Source getSource( final Config config )
  {
    return new ReadabilitySource(this, config);
  }

  @Override
  public SourceType getSourceType()
  {
    return SourceType.READABILITY;
  }

  @Override
  public String toString()
  {
    return "readability(" + token + ')';
  }

  String getKey()
  {
    return key;
  }

  String getSecret()
  {
    return secret;
  }

  Token getToken()
  {
    return token;
  }
}
