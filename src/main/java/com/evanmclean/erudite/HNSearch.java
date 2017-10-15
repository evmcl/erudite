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
 * Performs an API lookup of hn.algolia.com to see if the article has featured
 * on Hacker News and if so, returns the discussion thread URL.
 *
 * @author Evan M<sup>c</sup>Lean,
 *         <a href="http://evanmclean.com/" target="_blank">M<sup>c</sup>Lean
 *         Computer Services</a>
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

    @Override
    public String toString()
    {
      return "HNHits [hits=" + hits + "]";
    }

  }

  private static class HNItem implements Comparable<HNItem>
  {
    final String objectID;
    final String url;
    final int created_at_i;
    final int num_comments;

    @SuppressWarnings( "unused" )
    public HNItem( @JsonProperty( "objectID" ) final String objectID,
        @JsonProperty( "url" ) final String url //
        , @JsonProperty( "created_at_i" ) final int created_at_i //
        , @JsonProperty( "num_comments" ) final int num_comments //
    )
    {
      this.objectID = objectID;
      this.url = url;
      this.created_at_i = created_at_i;
      this.num_comments = num_comments;
    }

    @Override
    public int compareTo( final HNItem rhs )
    {
      int ret = rhs.num_comments - num_comments;
      if ( ret == 0 )
      {
        ret = created_at_i - rhs.created_at_i;
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
      if ( created_at_i != other.created_at_i )
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
      result = prime * result + created_at_i;
      result = prime * result + num_comments;
      result = prime * result + ((objectID == null) ? 0 : objectID.hashCode());
      result = prime * result + ((url == null) ? 0 : url.hashCode());
      return result;
    }

    @Override
    public String toString()
    {
      return "HNItem [objectID=" + objectID + ", url=" + url + "]";
    }
  }

  private static class Source
  {
    final String sourceUrl;
    final String title;

    Source( final String source_url, final String title )
    {
      this.sourceUrl = source_url;
      this.title = title;
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
      Source other = (Source) obj;
      if ( sourceUrl == null )
      {
        if ( other.sourceUrl != null )
          return false;
      }
      else if ( !sourceUrl.equals(other.sourceUrl) )
        return false;
      if ( title == null )
      {
        if ( other.title != null )
          return false;
      }
      else if ( !title.equals(other.title) )
        return false;
      return true;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((sourceUrl == null) ? 0 : sourceUrl.hashCode());
      result = prime * result + ((title == null) ? 0 : title.hashCode());
      return result;
    }
  }

  public static final ImmutableList<String> EMPTY = ImmutableList.of();
  private static final LoadingCache<Source, ImmutableList<String>> cache = createCache();

  /**
   * Removes all lookups in the cache.
   */
  public static void clearCache()
  {
    cache.invalidateAll();
  }

  /**
   * Finds the URLs of the discussion threads on Hacker News for a web page.
   *
   * @param article
   *        The Article to look up.
   * @return The URLs of the discussion threads on Hacker News, sorted by most
   *         comments, or an empty list.
   */
  public static ImmutableList<String> lookup( final Article article )
  {
    try
    {
      return cache
          .get(new Source(article.getOriginalUrl(), article.getTitle()));
    }
    catch ( Exception ex )
    {
      LoggerFactory.getLogger(HNSearch.class)
          .trace("Error while HNSearch for " + article.getOriginalUrl(), ex);
      return ImmutableList.of();
    }
  }

  private static LoadingCache<Source, ImmutableList<String>> createCache()
  {
    return CacheBuilder.newBuilder().maximumSize(20)
        .build(new CacheLoader<Source, ImmutableList<String>>() {
          @Override
          public ImmutableList<String> load( final Source source )
          {
            final Logger log = LoggerFactory.getLogger(HNSearch.class);
            try
            {
              log.trace("hn.algolia.com lookup: {} {}", source.sourceUrl,
                source.title);
              ImmutableList<String> results = search(source.sourceUrl,
                source.sourceUrl);
              if ( results.isEmpty() && Str.isNotEmpty(source.title) )
              {
                log.trace(
                  "Could not find match based on URL, so searching on title instead.");
                results = search(source.title, source.sourceUrl);
              }
              if ( results.isEmpty() )
                log.trace("No Hacker News discussion thread for {}",
                  source.sourceUrl);
              return results;
            }
            catch ( Exception ex )
            {
              log.trace("Exception while looking up hn.algolia.com for: "
                  + source.sourceUrl,
                ex);
              return EMPTY;
            }
          }

          @SuppressWarnings( "synthetic-access" )
          private ImmutableList<String> search( final String term,
              final String source_url )
          {
            try
            {
              final String hnsearch_url = "http://hn.algolia.com/api/v1/search_by_date?tags=story&query="
                  + Esc.url.text(term);
              final Connection conn = Conn.connect(hnsearch_url);
              conn.ignoreContentType(true);
              final Response resp = conn.execute();
              if ( resp.statusCode() != 200 )
              {
                LoggerFactory.getLogger(HNSearch.class).trace(
                  "GET {} returned {}: " + resp.statusMessage(), hnsearch_url,
                  String.valueOf(resp.statusCode()));
                return EMPTY;
              }

              // Parse json return.
              final String body = resp.body();
              return parse(body, source_url);
            }
            catch ( IOException ex )
            {
              LoggerFactory.getLogger(HNSearch.class).trace(
                "Exception while looking up hn.algolia.com for: " + term, ex);
              return EMPTY;
            }
          }
        });
  }

  private static ImmutableList<String> parse( final String json,
      final String source_url )
    throws JsonParseException,
      IOException
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
      return EMPTY;
    return ImmutableList.copyOf(items.values());
  }

  private HNSearch()
  {
    // empty
  }
}
