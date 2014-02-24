package com.evanmclean.erudite.pocket.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Content
{
  private final String html;
  private final List<Image> images;

  public Content( @JsonProperty( "article" ) final SubContent content )
  {
    this.html = content.getHtml();
    this.images = content.getImages();
  }

  public String getHtml()
  {
    return html;
  }

  public ImmutableList<Image> getImages()
  {
    return (ImmutableList<Image>) images;
  }

  @Override
  public String toString()
  {
    return "Content [html=" + html + ", images=" + images + "]";
  }
}
