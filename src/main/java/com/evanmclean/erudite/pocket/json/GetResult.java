package com.evanmclean.erudite.pocket.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class GetResult
{
  private final int complete;
  private final int status;
  private final long since;
  private final List<Article> articles;

  public GetResult( @JsonProperty( "complete" ) final int complete,
      @JsonProperty( "status" ) final int status,
      @JsonProperty( "since" ) final long since,
      @JsonProperty( "list" ) final List<Map<String, Article>> articles )
  {
    this.complete = complete;
    this.status = status;
    this.since = since;

    final ImmutableList.Builder<Article> bldr = ImmutableList.builder();
    for ( final Map<String, Article> map : articles )
      bldr.addAll(map.values());
    this.articles = bldr.build();
  }

  public ImmutableList<Article> getArticles()
  {
    return (ImmutableList<Article>) articles;
  }

  public int getComplete()
  {
    return complete;
  }

  public long getSince()
  {
    return since;
  }

  public int getStatus()
  {
    return status;
  }

  @Override
  public String toString()
  {
    return "GetResult [complete=" + complete + ", status=" + status
        + ", since=" + since + ", articles=" + articles + "]";
  }
}
