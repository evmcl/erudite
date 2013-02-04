package com.evanmclean.erudite;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.misc.Conn;
import com.evanmclean.evlib.escape.Esc;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Performs an API lookup of HNSearch.com to see if the article has featured on
 * Hacker News and if so, returns the discussion thread URL.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class HNSearch
{
  private static final String NO_HN_URL = "Marker for no HN URL";
  private static final LoadingCache<String, String> cache = createCache();

  /**
   * Removes all lookups in the cache.
   */
  public static void clearCache()
  {
    cache.invalidateAll();
  }

  public static String lookup( final Article article )
  {
    return lookup(article.getOriginalUrl());
  }

  /**
   * Finds the URL of the discussion thread on Hacker News for a web page.
   * 
   * @param source_url
   *        The URL to look up.
   * @return The URL of the discussion thread on Hacker News, or
   *         <code>null</code> if there is not one, or it is the same as the
   *         <code>source_url</code>.
   */
  public static String lookup( final String source_url )
  {
    try
    {
      final String ret = cache.get(source_url);
      return NO_HN_URL.equals(ret) ? null : ret;
    }
    catch ( Exception ex )
    {
      LoggerFactory.getLogger(HNSearch.class).trace(
        "Error while HNSearch for " + source_url, ex);
      return null;
    }
  }

  private static LoadingCache<String, String> createCache()
  {
    return CacheBuilder.newBuilder().maximumSize(20)
        .build(new CacheLoader<String, String>() {
          @Override
          public String load( final String source_url )
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
                return NO_HN_URL;
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
                return NO_HN_URL;
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
                return NO_HN_URL;
              }

              // Now look for objects with field name of item.
              while ( token != null )
              {
                while ( (token = parser.nextValue()) != null )
                  if ( (token == JsonToken.START_OBJECT)
                      && "item".equals(parser.getCurrentName()) )
                    break;

                if ( token != null )
                {
                  String id = null;
                  String url = null;
                  while ( (token = parser.nextValue()) != null )
                  {
                    if ( token == JsonToken.END_OBJECT )
                      break;
                    if ( "id".equals(parser.getCurrentName()) )
                      id = parser.getText();
                    else if ( "url".equals(parser.getCurrentName()) )
                      url = parser.getText();
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
                      return hn_surl;
                    }

                    log.trace(
                      "Looks like {} is a Hacker News discussion thread.",
                      source_url);
                  }
                }
              }

              log.trace("No Hacker News discussion thread for {}", source_url);
              return NO_HN_URL;
            }
            catch ( IOException ex )
            {
              log.trace("Exception while looking up hnsearch.com for: "
                  + source_url, ex);
              return NO_HN_URL;
            }
          }
        });
  }

  private HNSearch()
  {
    // empty
  }
}
