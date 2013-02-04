package com.evanmclean.erudite.readability.json;

import org.jsoup.nodes.TextNode;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Article
{
  private final String id;
  private final String title;
  private final String url;
  private final String author;
  private final String excerpt;

  public Article( @JsonProperty( "id" ) final String id,
      @JsonProperty( "title" ) final String title,
      @JsonProperty( "url" ) final String url,
      @JsonProperty( "author" ) final String author,
      @JsonProperty( "excerpt" ) final String excerpt )
  {
    this.id = id;
    this.title = title;
    this.url = url;
    this.author = author;
    this.excerpt = TextNode.createFromEncoded(excerpt, null).getWholeText();
  }

  public String getAuthor()
  {
    return author;
  }

  public String getExcerpt()
  {
    return excerpt;
  }

  public String getId()
  {
    return id;
  }

  public String getTitle()
  {
    return title;
  }

  public String getUrl()
  {
    return url;
  }

  @Override
  public String toString()
  {
    return "Article [id=" + id + ", title=" + title + ", url=" + url
        + ", author=" + author + ", excerpt=" + excerpt + "]";
  }
}
