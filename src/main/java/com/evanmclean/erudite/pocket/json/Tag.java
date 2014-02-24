package com.evanmclean.erudite.pocket.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tag
{
  private final String tag;

  public Tag( @JsonProperty( "tag" ) final String tag )
  {
    this.tag = tag;
  }

  public String getTag()
  {
    return tag;
  }

  @Override
  public String toString()
  {
    return tag;
  }
}
