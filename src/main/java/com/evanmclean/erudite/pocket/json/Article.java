package com.evanmclean.erudite.pocket.json;

import java.util.Map;
import java.util.TreeSet;

import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.CompareCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;

public class Article
{
  private static ImmutableSortedSet<String> getTagSet(
      final Map<String, Tag> map )
  {
    if ( map == null )
      return ImmutableSortedSet.of();
    final TreeSet<String> set = new TreeSet<String>(CompareCase.INSTANCE);
    for ( final Tag tag : map.values() )
      set.add(tag.getTag());
    return ImmutableSortedSet.copyOfSorted(set);
  }

  private final String itemId;
  private final String resolvedId;
  private final String url;
  private final String title;
  private final String excerpt;
  private final boolean favourite;
  private final boolean isArticle;
  private final boolean isUnprocessed;
  private final ImmutableSortedSet<String> tags;

  public Article( @JsonProperty( "item_id" ) final String item_id,
      @JsonProperty( "resolved_id" ) final String resolved_id,
      @JsonProperty( "given_url" ) final String given_url,
      @JsonProperty( "resolved_url" ) final String resolved_url,
      @JsonProperty( "given_title" ) final String given_title,
      @JsonProperty( "resolved_title" ) final String resolved_title,
      @JsonProperty( "excerpt" ) final String excerpt,
      @JsonProperty( "favorite" ) final String favourite,
      @JsonProperty( "has_image" ) final String has_image,
      @JsonProperty( "has_video" ) final String has_video,
      @JsonProperty( "is_article" ) final String is_article,
      @JsonProperty( "is_index" ) final String is_index,
      @JsonProperty( "word_count" ) final String word_count,
      @JsonProperty( "tags" ) final Map<String, Tag> tags )
  {
    this.itemId = item_id;
    this.resolvedId = resolved_id;
    this.url = Str.ifEmpty(resolved_url, given_url);
    this.title = Str.ifEmpty(resolved_title, given_title);
    this.excerpt = excerpt;
    this.favourite = Str.notEquals(favourite, "0");
    this.isArticle = Str.notEquals(is_article, "0");
    this.tags = getTagSet(tags);
    this.isUnprocessed = Str.equals(is_article, "0")
        && Str.equals(is_index, "0") && Str.equals(has_image, "0")
        && Str.equals(has_video, "0") && Str.equals(word_count, "0");
  }

  public String getExcerpt()
  {
    return excerpt;
  }

  public String getItemId()
  {
    return itemId;
  }

  public String getResolvedId()
  {
    return resolvedId;
  }

  public ImmutableSortedSet<String> getTags()
  {
    return tags;
  }

  public String getTitle()
  {
    return title;
  }

  public String getUrl()
  {
    return url;
  }

  public boolean isFavourite()
  {
    return favourite;
  }

  public boolean isUnprocessed()
  {
    return isUnprocessed && isValid();
  }

  public boolean isUsable()
  {
    return isArticle && isValid();
  }

  private boolean isValid()
  {
    if ( Str.isEmpty(itemId) || Str.isEmpty(resolvedId) || Str.isEmpty(url)
        || Str.isEmpty(title) )
      return false;
    // If resolved_id = 0 then hasn't been processed by readability yet, so
    // should skip.
    if ( Str.equals(resolvedId, "0") )
      return false;
    return true;
  }
}
