package com.evanmclean.erudite.pocket;

import java.util.Map;

import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.erudite.sessions.SourceType;
import com.google.common.collect.ImmutableMap;

/**
 * Session object for Pocket.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class PocketSession implements Session
{
  private static final long serialVersionUID = -8577131670126950423L;

  private final String key;
  private final String token;
  private final ImmutableMap<String, String> cookies;

  PocketSession( final String key, final String token,
      final ImmutableMap<String, String> cookies )
  {
    this.key = key;
    this.token = token;
    this.cookies = cookies;
  }

  public ImmutableMap<String, String> getCookies()
  {
    return cookies;
  }

  @Override
  public Source getSource( final Config config )
  {
    return new PocketSource(this, config);
  }

  @Override
  public SourceType getSourceType()
  {
    return SourceType.POCKET;
  }

  @Override
  public String toString()
  {
    final StringBuilder buff = new StringBuilder("pocket(").append(token);
    for ( Map.Entry<String, String> entry : cookies.entrySet() )
      buff.append(", ").append(entry.getKey()).append('=')
          .append(entry.getValue());
    buff.append(')');
    return buff.toString();
  }

  String getKey()
  {
    return key;
  }

  String getToken()
  {
    return token;
  }
}
