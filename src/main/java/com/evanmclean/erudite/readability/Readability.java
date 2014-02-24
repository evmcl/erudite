package com.evanmclean.erudite.readability;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.config.TitleMunger;
import com.evanmclean.erudite.readability.json.Bookmark;
import com.evanmclean.erudite.readability.json.Bookmarks;
import com.evanmclean.erudite.readability.json.Content;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.lang.Str;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Represents a login to the Readability service.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class Readability
{
  public static class Authoriser
  {
    private final String key;
    private final String secret;
    private final OAuthService service;
    private Token reqToken;

    @SuppressWarnings( "synthetic-access" )
    Authoriser( final String key, final String secret )
    {
      this.key = key;
      this.secret = secret;
      this.service = createOAuthService(key, secret);
    }

    public ReadabilitySession getSession( final String verifier_key )
    {
      final Token token = service.getAccessToken(reqToken, new Verifier(
          verifier_key));
      return new ReadabilitySession(key, secret, token);
    }

    public String getUrl()
    {
      reqToken = service.getRequestToken();
      return new ReadabilityApi().getAuthorizationUrl(reqToken);
    }
  }

  interface Filter
  {
    String getApiArgs();

    boolean isFiltered( Bookmark bookmark );
  }

  private class Article implements ReadabilityArticle
  {
    private final String articleId;
    private final String bookmarkId;
    private final String title;
    private final String originalUrl;
    private final String summary;
    private final ImmutableMap<String, String> tags;
    private Element _text;

    Article( final String article_id, final String bookmark_id,
        final String title, final String original_url, final String summary,
        final ImmutableMap<String, String> tags )
    {
      this.articleId = article_id;
      this.bookmarkId = bookmark_id;
      this.title = title;
      this.originalUrl = original_url;
      this.summary = summary;
      this.tags = tags;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void addTags( final String add_tags ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Adding tags {} to article on Readability: {}", add_tags, title);
      request(Verb.POST //
        , BASE_URL + "/bookmarks/" + Esc.url.text(bookmarkId) + "/tags" //
        , ImmutableMap.of("tags", add_tags)).close();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void archive() throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Archiving article on Readability: {}", title);
      request(Verb.POST //
        , BASE_URL + "/bookmarks/" + Esc.url.text(bookmarkId) //
        , ImmutableMap.of("archive", "1")).close();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void favourite( final boolean favourite ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "{} favourite flag for article on Readability: {}",
        favourite ? "Setting" : "Clearing", title);
      request(Verb.POST //
        , BASE_URL + "/bookmarks/" + Esc.url.text(bookmarkId) //
        , ImmutableMap.of("favorite", favourite ? "1" : "0")).close();
    }

    @Override
    public String getOriginalUrl()
    {
      return originalUrl;
    }

    @Override
    public String getSourceUrl()
    {
      return "http://rdd.me/" + Esc.url.text(articleId);
    }

    @Override
    public String getSummary()
    {
      return summary;
    }

    @Override
    public String getTitle()
    {
      return title;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void remove() throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Removing article on Readability: {}", title);
      request(Verb.DELETE, BASE_URL + "/bookmarks/" + Esc.url.text(bookmarkId))
          .close();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void removeTag( final String tag ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Removing tag {} for article on Readability: {}", tag, title);
      final String id = tags.get(tag);
      if ( id != null )
        request(
          Verb.DELETE,
          BASE_URL + "/bookmarks/" + Esc.url.text(bookmarkId) + "/tags/"
              + Esc.url.text(id)).close();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public Element text() throws IOException
    {
      Element text = _text;
      if ( text == null )
      {
        LoggerFactory.getLogger(getClass()).trace(
          "Retrieving article from Readability: {}", title);

        final InputStream resp = request(Verb.GET, BASE_URL + "/articles/"
            + articleId);
        final String content = json.readValue(resp, Content.class).getContent();
        if ( Str.isEmpty(content) )
          throw new IOException("No content for article: " + title);
        final Document doc = Parser.parseBodyFragment(content, getSourceUrl());
        final Elements bodies = doc.getElementsByTag("body");
        if ( bodies.size() != 1 )
        {
          final Logger log = LoggerFactory.getLogger(getClass());
          log.trace("Error converting content: {}", content);
          log.trace("More than one body tag.");
        }
        final List<Node> nodes = bodies.get(0).childNodes();
        switch ( nodes.size() )
        {
          case 0:
            throw new IOException("No content for article.");

          case 1:
          {
            Node node = nodes.get(0);
            if ( node instanceof Element )
            {
              text = (Element) node;
              break;
            }
          }
          //$FALL-THROUGH$

          default:
          {
            text = new Element(Tag.valueOf("div"), Str.EMPTY);
            // (Use defensive copy to avoid a ConcurrentModificationException)
            for ( Node node : ImmutableList.copyOf(nodes) )
              text.appendChild(node);
          }
            break;
        }
        _text = text;
      }

      return text.clone();
    }

    @Override
    public String toString()
    {
      final StringBuilder buff = new StringBuilder(title);

      buff.append("\n  ").append(originalUrl) //
      ;

      if ( Str.isNotEmpty(summary) )
        buff.append("\n\n  ").append(summary);
      return buff.toString();
    }
  }

  public static String API_KEY_URL = "https://www.readability.com/account/connections/#request-api-key";

  static final String BASE_URL = "https://www.readability.com/api/rest/v1";

  public static Authoriser getAuthoriser( final String key, final String secret )
  {
    return new Authoriser(key, secret);
  }

  private static OAuthService createOAuthService( final String key,
      final String secret )
  {
    return new ServiceBuilder().provider(ReadabilityApi.class).apiKey(key)
        .apiSecret(secret).build();
  }

  private final OAuthService service;
  private final Token token;
  private final TitleMunger titleMunger;
  private final Filter filter;
  private final ObjectMapper json = new ObjectMapper();
  private ImmutableList<ReadabilityArticle> _articles;

  /**
   * Create a logged-in connection to Readability based on a session previously
   * produced by {@link Authoriser}.
   * 
   * @param session
   *        The session object to use.
   * @param filter
   * @param title_munger
   *        A title munger to use on article titles.
   */
  public Readability( final Session session, final Filter filter,
      final TitleMunger title_munger )
  {
    if ( !(session instanceof ReadabilitySession) )
      throw new IllegalArgumentException(
          "Invalid session object for Readability.");

    final ReadabilitySession rsession = (ReadabilitySession) session;

    this.titleMunger = (title_munger != null) ? title_munger : TitleMunger
        .empty();

    this.filter = (filter != null) ? filter : new Filter() {
      @Override
      public String getApiArgs()
      {
        return null;
      }

      @Override
      public boolean isFiltered( final Bookmark bookmark )
      {
        return false;
      }
    };

    this.service = createOAuthService(rsession.getKey(), rsession.getSecret());
    this.token = rsession.getToken();

    json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public ImmutableList<ReadabilityArticle> getArticles() throws IOException
  {
    ImmutableList<ReadabilityArticle> articles = _articles;
    if ( articles == null )
      _articles = articles = _getArticles();
    return articles;
  }

  private ImmutableList<ReadabilityArticle> _getArticles() throws IOException
  {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.trace("Retrieving article list from Readability.");

    final ImmutableList.Builder<ReadabilityArticle> bldr = ImmutableList
        .builder();

    long num_pages = 1L;
    long page_num = 0L;
    while ( page_num < num_pages )
    {
      final InputStream resp = request(Verb.GET, getBookmarksUrl(++page_num));

      final Bookmarks bookmarks = json.readValue(resp, Bookmarks.class);
      num_pages = bookmarks.getMeta().getNumPages();

      for ( final Bookmark bookmark : bookmarks.getBookmarks() )
        if ( !filter.isFiltered(bookmark) )
          bldr.add(new Article(bookmark.getArticle().getId(), bookmark.getId(),
              titleMunger.munge(bookmark.getArticle().getTitle()), bookmark
                  .getArticle().getUrl(), bookmark.getArticle().getExcerpt(),
              bookmark.getTags()));
    }

    return bldr.build();
  }

  private String getBookmarksUrl( final long page_num )
  {
    String url = BASE_URL + "/bookmarks?archive=0&per_page=50&page=" + page_num;
    final String args = filter.getApiArgs();
    if ( Str.isNotEmpty(args) )
      url += '&' + args;
    return url;
  }

  private InputStream request( final Verb verb, final String url )
    throws IOException
  {
    return request(verb, url, null);
  }

  private InputStream request( final Verb verb, final String url,
      final Map<String, String> post_data ) throws IOException
  {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.trace("{} {}", verb, url);
    if ( post_data != null )
      log.trace("Posting: {}", post_data);

    final OAuthRequest req = new OAuthRequest(verb, url);
    if ( post_data != null )
      for ( final Map.Entry<String, String> entry : post_data.entrySet() )
        req.addBodyParameter(entry.getKey(), entry.getValue());
    service.signRequest(token, req);
    final Response resp = req.send();
    log.trace("Response {}: {}", resp.isSuccessful() ? "OK" : "Error",
      resp.getCode());
    if ( !resp.isSuccessful() )
    {
      for ( final Map.Entry<String, String> entry : resp.getHeaders()
          .entrySet() )
        log.trace("Header {}: {}", entry.getKey(), entry.getValue());
      log.error("Readability API call failed: {} {}", verb, url);
      throw new IOException("Readability API call failed. See log.");
    }
    return resp.getStream();
  }
}
