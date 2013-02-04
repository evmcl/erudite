package com.evanmclean.erudite.readability.json;

import com.evanmclean.evlib.util.Colls;
import com.evanmclean.evlib.util.TreeMapIgnoreCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

public class Bookmark
{
  private final String id;
  private final ImmutableMap<String, String> tags;
  private final boolean favourite;
  private final boolean archive;
  private final Article article;

  public Bookmark( @JsonProperty( "id" ) final String id,
      @JsonProperty( "tags" ) final Tag[] tags,
      @JsonProperty( "favorite" ) final boolean favourite,
      @JsonProperty( "archive" ) final boolean archive,
      @JsonProperty( "article" ) final Article article )
  {
    this.id = id;
    this.favourite = favourite;
    this.archive = archive;
    this.article = article;

    final TreeMapIgnoreCase<String> map = Colls.newTreeMapIgnoreCase();
    for ( final Tag tag : tags )
      map.put(tag.getText(), tag.getId());
    this.tags = ImmutableSortedMap.copyOfSorted(map);
  }

  public Article getArticle()
  {
    return article;
  }

  public String getId()
  {
    return id;
  }

  public ImmutableMap<String, String> getTags()
  {
    return tags;
  }

  public boolean isArchive()
  {
    return archive;
  }

  public boolean isFavourite()
  {
    return favourite;
  }

  @Override
  public String toString()
  {
    return "Bookmark [id=" + id + ", tags=" + tags + ", favourite=" + favourite
        + ", archive=" + archive + ", article=" + article + "]";
  }
}
