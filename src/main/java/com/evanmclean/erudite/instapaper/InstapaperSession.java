package com.evanmclean.erudite.instapaper;

import java.util.Map;

import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.erudite.sessions.SourceType;
import com.google.common.collect.ImmutableMap;

/**
 * Session object for Instapaper.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class InstapaperSession implements Session
{
  private static final long serialVersionUID = -8577141670126950423L;

  private final ImmutableMap<String, String> cookies;

  InstapaperSession( final ImmutableMap<String, String> cookies )
  {
    this.cookies = cookies;
  }

  @Override
  public Source getSource( final Config config )
  {
    return new InstapaperSource(this, config);
  }

  @Override
  public SourceType getSourceType()
  {
    return SourceType.INSTAPAPER;
  }

  @Override
  public String toString()
  {
    final StringBuilder buff = new StringBuilder("instapaper(");
    boolean first = true;
    for ( Map.Entry<String, String> entry : cookies.entrySet() )
    {
      if ( first )
        first = false;
      else
        buff.append(", ");
      buff.append(entry.getKey()).append('=').append(entry.getValue());
    }
    buff.append(')');
    return buff.toString();
  }

  ImmutableMap<String, String> getCookies()
  {
    return cookies;
  }
}
