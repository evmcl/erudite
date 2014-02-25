package com.evanmclean.erudite;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.misc.Conn;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.lang.Str;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

/**
 * Performs an API lookup of HNSearch.com to see if the article has featured on
 * Hacker News and if so, returns the discussion thread URL.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class HNSearch
{
  private static class HNHits
  {
    final List<HNItem> hits;

    @SuppressWarnings( "unused" )
    public HNHits( @JsonProperty( "hits" ) final List<HNItem> hits )
    {
      this.hits = hits;
    }

  }

  private static class HNItem implements Comparable<HNItem>
  {
    final String objectID;
    final String url;
    final int create_at_i;
    final int num_comments;

    @SuppressWarnings( "unused" )
    public HNItem( @JsonProperty( "objectID" ) final String objectID,
        @JsonProperty( "url" ) final String url //
        , @JsonProperty( "create_at_i" ) final int create_at_i //
        , @JsonProperty( "num_comments" ) final int num_comments //
    )
    {
      this.objectID = objectID;
      this.url = url;
      this.create_at_i = create_at_i;
      this.num_comments = num_comments;
    }

    @Override
    public int compareTo( final HNItem rhs )
    {
      int ret = rhs.num_comments - num_comments;
      if ( ret == 0 )
      {
        ret = create_at_i - rhs.create_at_i;
        if ( ret == 0 )
          ret = Str.ifNull(objectID).compareTo(Str.ifNull(rhs.objectID));
      }
      return ret;
    }

    @Override
    public boolean equals( final Object obj )
    {
      if ( this == obj )
        return true;
      if ( obj == null )
        return false;
      if ( getClass() != obj.getClass() )
        return false;
      HNItem other = (HNItem) obj;
      if ( create_at_i != other.create_at_i )
        return false;
      if ( num_comments != other.num_comments )
        return false;
      if ( objectID == null )
      {
        if ( other.objectID != null )
          return false;
      }
      else if ( !objectID.equals(other.objectID) )
        return false;
      if ( url == null )
      {
        if ( other.url != null )
          return false;
      }
      else if ( !url.equals(other.url) )
        return false;
      return true;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + create_at_i;
      result = prime * result + num_comments;
      result = prime * result + ((objectID == null) ? 0 : objectID.hashCode());
      result = prime * result + ((url == null) ? 0 : url.hashCode());
      return result;
    }
  }

  public static final ImmutableList<String> EMPTY = ImmutableList.of();
  private static final LoadingCache<String, ImmutableList<String>> cache = createCache();

  /**
   * Removes all lookups in the cache.
   */
  public static void clearCache()
  {
    cache.invalidateAll();
  }

  public static ImmutableList<String> lookup( final Article article )
  {
    return lookup(article.getOriginalUrl());
  }

  /**
   * Finds the URLs of the discussion threads on Hacker News for a web page.
   * 
   * @param source_url
   *        The URL to look up.
   * @return The URLs of the discussion threads on Hacker News, sorted by most
   *         comments, or an empty list.
   */
  public static ImmutableList<String> lookup( final String source_url )
  {
    try
    {
      return cache.get(source_url);
    }
    catch ( Exception ex )
    {
      LoggerFactory.getLogger(HNSearch.class).trace(
        "Error while HNSearch for " + source_url, ex);
      return ImmutableList.of();
    }
  }

  private static LoadingCache<String, ImmutableList<String>> createCache()
  {
    return CacheBuilder.newBuilder().maximumSize(20)
        .build(new CacheLoader<String, ImmutableList<String>>() {
          @SuppressWarnings( "synthetic-access" )
          @Override
          public ImmutableList<String> load( final String source_url )
          {
            final Logger log = LoggerFactory.getLogger(HNSearch.class);
            try
            {
              log.trace("HNSearch.com lookup: {}", source_url);
              final String hnsearch_url = "http://hn.algolia.com/api/v1/search?tags=story&query="
                  + Esc.url.text(source_url);
              final Connection conn = Conn.connect(hnsearch_url);
              conn.ignoreContentType(true);
              final Response resp = conn.execute();
              if ( resp.statusCode() != 200 )
              {
                log.trace("GET {} returned {}: " + resp.statusMessage(),
                  hnsearch_url, String.valueOf(resp.statusCode()));
                return EMPTY;
              }

              // Parse json return.
              final String body = resp.body();
              return parse(body, source_url);
            }
            catch ( IOException ex )
            {
              ex.printStackTrace(); // emmark
              log.trace("Exception while looking up hnsearch.com for: "
                  + source_url, ex);
              return EMPTY;
            }
          }
        });
  }

  private static ImmutableList<String> parse( final String json,
      final String source_url ) throws JsonParseException, IOException
  {
    final Logger log = LoggerFactory.getLogger(HNSearch.class);

    final ObjectMapper om = new ObjectMapper();
    om.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final HNHits hits = om.readValue(json, HNHits.class);
    final TreeMap<HNItem, String> items = new TreeMap<HNItem, String>();
    for ( final HNItem item : hits.hits )
      if ( source_url.equals(item.url) && Str.isNotEmpty(item.objectID) )
      {
        final String hn_url = "http://news.ycombinator.com/item?id="
            + item.objectID;
        final String hn_surl = "https://news.ycombinator.com/item?id="
            + item.objectID;
        if ( (!source_url.equalsIgnoreCase(hn_url))
            && (!source_url.equalsIgnoreCase(hn_surl)) )
        {
          log.trace("Hacker News discussion for {} is {}", source_url, hn_surl);
          items.put(item, hn_surl);
        }
      }
    if ( items.isEmpty() )
    {
      log.trace("No Hacker News discussion thread for {}", source_url);
      return EMPTY;
    }
    return ImmutableList.copyOf(items.values());
  }

  private HNSearch()
  {
    // empty
  }
}
