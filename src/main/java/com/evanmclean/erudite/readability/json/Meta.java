package com.evanmclean.erudite.readability.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Meta
{
  private final long numPages;

  public Meta( @JsonProperty( "num_pages" ) final long numPages )
  {
    this.numPages = numPages;
  }

  public long getNumPages()
  {
    return numPages;
  }

  @Override
  public String toString()
  {
    return "Meta [numPages=" + numPages + "]";
  }
}
