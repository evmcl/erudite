package com.evanmclean.erudite;

import java.io.IOException;
import java.util.TreeMap;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.misc.Conn;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.misc.Conv;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
  private static class HNItem implements Comparable<HNItem>
  {
    private final String id;
    private final String createTs;
    private final int numComments;

    public HNItem( final String id, final String create_ts,
        final int num_comments )
    {
      this.id = id;
      this.createTs = create_ts;
      this.numComments = num_comments;
    }

    @Override
    public int compareTo( final HNItem rhs )
    {
      int ret = rhs.numComments - numComments;
      if ( ret == 0 )
      {
        ret = Str.ifNull(createTs).compareTo(Str.ifNull(rhs.createTs));
        if ( ret == 0 )
          ret = Str.ifNull(id).compareTo(Str.ifNull(rhs.id));
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
      if ( numComments != other.numComments )
        return false;
      if ( id == null )
      {
        if ( other.id != null )
          return false;
      }
      else if ( !id.equals(other.id) )
        return false;
      if ( createTs == null )
      {
        if ( other.createTs != null )
          return false;
      }
      else if ( !createTs.equals(other.createTs) )
        return false;
      return true;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((createTs == null) ? 0 : createTs.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + numComments;
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
          @Override
          public ImmutableList<String> load( final String source_url )
          {
            final Logger log = LoggerFactory.getLogger(HNSearch.class);
            try
            {
              log.trace("HNSearch.com lookup: {}", source_url);
              final String hnsearch_url = "http://api.thriftdb.com/api.hnsearch.com/items/_search?filter[fields][url]="
                  + Esc.url.text(source_url);
              final Connection conn = Conn.connect(hnsearch_url);
              final Response resp = conn.execute();
              if ( resp.statusCode() != 200 )
              {
                log.trace("GET {} returned {}: " + resp.statusMessage(),
                  hnsearch_url, String.valueOf(resp.statusCode()));
                return EMPTY;
              }

              // Parse json return.
              final String body = resp.body();
              final JsonParser parser = new JsonFactory()
                  .createJsonParser(body);

              // Ensure we are at the start of a JSON object.
              if ( parser.nextToken() != JsonToken.START_OBJECT )
              {
                log.trace("Returned data from {} not a JSON object: {}",
                  hnsearch_url, body);
                return EMPTY;
              }

              // Find the results array.
              JsonToken token;
              while ( (token = parser.nextValue()) != null )
                if ( (token == JsonToken.START_ARRAY)
                    && "results".equals(parser.getCurrentName()) )
                  break;

              if ( token == null )
              {
                log.trace(
                  "Could not find results array in JSON object from {}: {}",
                  hnsearch_url, body);
                return EMPTY;
              }

              // Now look for objects with field name of item.
              while ( token != null )
              {
                final TreeMap<HNItem, String> items = new TreeMap<HNItem, String>();
                while ( (token = parser.nextValue()) != null )
                  if ( (token == JsonToken.START_OBJECT)
                      && "item".equals(parser.getCurrentName()) )
                  {
                    String id = null;
                    String url = null;
                    String create_ts = null;
                    int num_comments = -2;
                    while ( (token = parser.nextValue()) != null )
                    {
                      if ( token == JsonToken.END_OBJECT )
                        break;
                      if ( "id".equals(parser.getCurrentName()) )
                        id = parser.getText();
                      else if ( "url".equals(parser.getCurrentName()) )
                        url = parser.getText();
                      else if ( "create_ts".equals(parser.getCurrentName()) )
                        create_ts = parser.getText();
                      else if ( "num_comments".equals(parser.getCurrentName()) )
                        num_comments = Conv.toInt(parser.getText(), -1);
                    }

                    if ( source_url.equals(url) && (id != null) )
                    {
                      // Found a match!
                      final String hn_url = "http://news.ycombinator.com/item?id="
                          + id;
                      final String hn_surl = "https://news.ycombinator.com/item?id="
                          + id;
                      if ( (!source_url.equalsIgnoreCase(hn_url))
                          && (!source_url.equalsIgnoreCase(hn_surl)) )
                      {
                        log.trace("Hacker News discussion for {} is {}",
                          source_url, hn_surl);
                        items.put(new HNItem(id, create_ts, num_comments),
                          hn_surl);
                      }
                    }
                  }
                if ( items.isEmpty() )
                  return EMPTY;
                return ImmutableList.copyOf(items.values());
              }

              log.trace("No Hacker News discussion thread for {}", source_url);
              return EMPTY;
            }
            catch ( IOException ex )
            {
              log.trace("Exception while looking up hnsearch.com for: "
                  + source_url, ex);
              return EMPTY;
            }
          }
        });
  }

  private HNSearch()
  {
    // empty
  }
}
