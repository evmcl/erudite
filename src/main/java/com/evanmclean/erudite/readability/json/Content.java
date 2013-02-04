package com.evanmclean.erudite.readability.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Content
{
  private final String content;

  public Content( @JsonProperty( "content" ) final String content )
  {
    this.content = content;
  }

  public String getContent()
  {
    return content;
  }

  @Override
  public String toString()
  {
    return "Content [content=" + content + "]";
  }
}
