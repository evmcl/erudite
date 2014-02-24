package com.evanmclean.erudite.pocket.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Image
{
  private final String id;
  private final String src;

  public Image( @JsonProperty( "image_id" ) final String id,
      @JsonProperty( "src" ) final String src )
  {
    this.id = id;
    this.src = src;
  }

  public String getId()
  {
    return id;
  }

  public String getSrc()
  {
    return src;
  }

  @Override
  public String toString()
  {
    return "Image [id=" + id + ", src=" + src + "]";
  }
}
