package com.evanmclean.erudite.readability.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Bookmarks
{
  private final ImmutableList<Bookmark> bookmarks;
  private final Meta meta;

  public Bookmarks( @JsonProperty( "bookmarks" ) final Bookmark[] bookmarks,
      @JsonProperty( "meta" ) final Meta meta )
  {
    this.bookmarks = ImmutableList.copyOf(bookmarks);
    this.meta = meta;
  }

  public ImmutableList<Bookmark> getBookmarks()
  {
    return bookmarks;
  }

  public Meta getMeta()
  {
    return meta;
  }

  @Override
  public String toString()
  {
    return "Bookmarks [bookmarks=" + bookmarks + ", meta=" + meta + "]";
  }
}
