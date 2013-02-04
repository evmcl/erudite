package com.evanmclean.erudite.readability.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tag
{
  private final String id;
  private final String text;

  public Tag( @JsonProperty( "id" ) final String id,
      @JsonProperty( "text" ) final String text )
  {
    this.id = id;
    this.text = text;
  }

  public String getId()
  {
    return id;
  }

  public String getText()
  {
    return text;
  }

  @Override
  public String toString()
  {
    return "Tag [id=" + id + ", text=" + text + "]";
  }
}
