package com.evanmclean.erudite.pocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.config.TitleMunger;
import com.evanmclean.erudite.misc.Conn;
import com.evanmclean.erudite.pocket.json.ActionResults;
import com.evanmclean.erudite.pocket.json.Content;
import com.evanmclean.erudite.pocket.json.GetResult;
import com.evanmclean.erudite.pocket.json.Image;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.evlib.charset.Charsets;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.exceptions.UnhandledException;
import com.evanmclean.evlib.io.ByteArrayInputOutputStream;
import com.evanmclean.evlib.lang.Str;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

/**
 * Represents a login to the Pocket service.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class Pocket
{
  public static class Authoriser
  {
    private final String key;
    private final String code;
    private final ImmutableMap<String, String> cookies;

    Authoriser( final String key, final String code,
        final ImmutableMap<String, String> cookies )
    {
      this.key = key;
      this.code = code;
      this.cookies = cookies;
    }

    public PocketSession getSession() throws IOException
    {
      final String url = API_BASE_URL + "/oauth/authorize";
      final Connection conn = Conn.connect(url);
      conn.method(Connection.Method.POST);
      conn.ignoreHttpErrors(true);
      conn.ignoreContentType(true);
      conn.data(ImmutableMap.of("consumer_key", key, "code", code));
      final Response resp = conn.execute();
      if ( resp.statusCode() != 200 )
        throw new IOException("POST " + url + " returned " + resp.statusCode()
            + ": " + resp.statusMessage());
      final String ret = Str.trimToEmpty(resp.body());
      Matcher mat = Pattern.compile("access_token=([^&]+)&username=.+")
          .matcher(ret);
      if ( !mat.matches() )
        throw new IOException("Unexpected data returned from authorize URL: "
            + ret);
      final String token = mat.group(1);
      return new PocketSession(key, token, cookies);
    }

    public String getUrl()
    {
      return BASE_URL + "auth/authorize?request_token=" + Esc.url.text(code)
          + "&redirect_uri=" + Esc.url.text(BASE_URL);
    }
  }

  interface Filter
  {
    Map<String, String> getPostData();

    boolean isFiltered( com.evanmclean.erudite.pocket.json.Article article );
  }

  private class Article implements PocketArticle
  {
    private final String itemId;
    private final String title;
    private final String resolvedUrl;
    private final String summary;
    private Element _text;

    public Article( final String item_id, final String title,
        final String resolved_url, final String summary )
    {
      this.itemId = item_id;
      this.title = title;
      this.resolvedUrl = resolved_url;
      this.summary = summary;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void addTags( final List<String> add_tags ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Adding tags [{}] to article on Pocket: {}", Str.join(", ", add_tags),
        title);
      sendApiRequest(ImmutableMap.of("action", "tags_add", "item_id", itemId,
        "tags", add_tags));
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void archive() throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Archiving article on Pocket: {}", title);
      sendApiRequest(ImmutableMap.of("action", "archive", "item_id",
        (Object) itemId));
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void favourite( final boolean favourite ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace("{} article on Pocket: {}",
        favourite ? "Favouriting" : "Unfavouriting", title);
      sendApiRequest(ImmutableMap.of("action", favourite ? "favorite"
          : "unfavorite", "item_id", (Object) itemId));
    }

    @Override
    public String getOriginalUrl()
    {
      return resolvedUrl;
    }

    @Override
    public String getSourceUrl()
    {
      // No source URL available with Pocket.
      return null;
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
        "Removing article on Pocket: {}", title);
      sendApiRequest(ImmutableMap.of("action", "delete", "item_id",
        (Object) itemId));
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void removeTags( final List<String> remove_tags ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Removing tags [{}] for article on Pocket: {}",
        Str.join(", ", remove_tags), title);
      sendApiRequest(ImmutableMap.of("action", "tags_remove", "item_id",
        itemId, "tags", remove_tags));
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public Element text() throws IOException
    {
      Element text = _text;
      if ( text == null )
      {
        LoggerFactory.getLogger(getClass()).trace(
          "Retrieving article from Pocket: {}", title);
        final InputStream resp = scrapeRequest(BASE_URL + "a/x/getArticle.php",
          ImmutableMap.of("itemId", itemId, "formCheck",
            "4bdb3835218ad9c831cbf4417f0a79d2"));
        final Content content = readValue(resp, Content.class);
        final String html = content.getHtml();
        if ( Str.isEmpty(html) )
          throw new IOException("No content for article: " + title);
        final Document doc = Parser.parseBodyFragment(html, BASE_URL
            + "a/read/" + itemId);
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
        insertImages(text, content.getImages());
        _text = text;
      }
      return text;
    }

    private void insertImages( final Element text,
        final ImmutableList<Image> images )
    {
      for ( final Image image : images )
      {
        final Element el = text.getElementById("RIL_IMG_" + image.getId());
        if ( el != null )
        {
          final Element img = el.appendElement("img");
          img.attr("src", image.getSrc());
        }
      }
    }
  }

  static final String BASE_URL = "https://getpocket.com/";
  static final String API_BASE_URL = BASE_URL + "v3";

  public static String API_KEY_URL = BASE_URL + "developer/apps/new";

  public static Authoriser getAuthoriser( final String user, final String pass,
      final String key ) throws IOException
  {
    final ImmutableMap<String, String> cookies;
    {
      final Connection conn = Conn.connect(BASE_URL + "login_process");
      conn.data("feed_id", user, "password", pass);
      conn.method(Connection.Method.POST);

      Connection.Response resp = conn.execute();
      final ImmutableMap.Builder<String, String> bldr = ImmutableMap.builder();
      for ( final Map.Entry<String, String> entry : resp.cookies().entrySet() )
        if ( entry.getKey().startsWith("sess_") )
          bldr.put(entry.getKey(), entry.getValue());
      cookies = bldr.build();
      if ( cookies.isEmpty() )
        return null;
    }

    final String code;
    {
      final String url = API_BASE_URL + "/oauth/request";
      final Connection conn = Conn.connect(url);
      conn.method(Connection.Method.POST);
      conn.ignoreHttpErrors(true);
      conn.ignoreContentType(true);
      conn.data(ImmutableMap.of("consumer_key", key, "redirect_uri",
        "pocketapp1234:authorizationFinished"));
      final Response resp = conn.execute();
      if ( resp.statusCode() != 200 )
        throw new IOException("POST " + url + " returned " + resp.statusCode()
            + ": " + resp.statusMessage());
      final String ret = Str.trimToEmpty(resp.body());
      if ( !ret.startsWith("code=") )
        throw new IOException("Unexpected data returned from request URL: "
            + ret);
      code = ret.substring(5);
    }

    return new Authoriser(key, code, cookies);
  }

  private final String consumerKey;
  private final String accessToken;
  private final TitleMunger titleMunger;
  private final Filter filter;
  private final ObjectMapper json = new ObjectMapper();
  private final ImmutableMap<String, String> immutableGetArgs = ImmutableMap
      .of("detailType", "complete", "contentType", "article");
  private ImmutableMap<String, String> scrapeCookies;
  private ImmutableList<PocketArticle> _articles;

  /**
   * Create a logged-in connection to Pocket based on a session previously
   * produced by {@link Authoriser}.
   * 
   * @param session
   *        The session object to use.
   * @param filter
   * @param title_munger
   *        A title munger to use on article titles.
   */
  public Pocket( final Session session, final Filter filter,
      final TitleMunger title_munger )
  {
    if ( !(session instanceof PocketSession) )
      throw new IllegalArgumentException("Invalid session object for Pocket.");

    final PocketSession psession = (PocketSession) session;

    this.titleMunger = (title_munger != null) ? title_munger : TitleMunger
        .empty();

    this.filter = (filter != null) ? filter : new Filter() {
      @Override
      public Map<String, String> getPostData()
      {
        return ImmutableMap.of();
      }

      @Override
      public boolean isFiltered(
          final com.evanmclean.erudite.pocket.json.Article article )
      {
        return false;
      }
    };

    this.consumerKey = psession.getKey();
    this.accessToken = psession.getToken();
    this.scrapeCookies = psession.getCookies();

    json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    json.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
  }

  public ImmutableList<PocketArticle> getArticles() throws IOException
  {
    ImmutableList<PocketArticle> articles = _articles;
    if ( articles == null )
      _articles = articles = _getArticles();
    return articles;
  }

  private ImmutableList<PocketArticle> _getArticles() throws IOException
  {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.trace("Retrieving article list from Pocket.");

    final ImmutableList.Builder<PocketArticle> bldr = ImmutableList.builder();

    final InputStream resp = apiRequest(API_BASE_URL + "/get",
      combine(filter.getPostData(), immutableGetArgs));
    final GetResult get = readValue(resp, GetResult.class);

    for ( final com.evanmclean.erudite.pocket.json.Article article : get
        .getArticles() )
      if ( article.isUsable() && (!filter.isFiltered(article)) )
        bldr.add(new Article(article.getItemId(), titleMunger.munge(article
            .getTitle()), article.getUrl(), article.getExcerpt()));

    return bldr.build();
  }

  private InputStream apiRequest( final String url,
      final Map<String, String> post_data ) throws IOException
  {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.trace("POST {} data: {}", url, post_data);

    final Connection conn = Conn.connect(url);
    conn.method(Connection.Method.POST);
    conn.ignoreHttpErrors(true);
    conn.ignoreContentType(true);
    conn.data("consumer_key", consumerKey, "access_token", accessToken);
    conn.data(post_data);
    final Response resp = conn.execute();
    log.trace("Response {}: {}", resp.statusCode(), resp.statusMessage());
    if ( resp.statusCode() != 200 )
    {
      for ( final Map.Entry<String, String> entry : resp.headers().entrySet() )
        log.trace("Header {}: {}", entry.getKey(), entry.getValue());
      log.error("Pocket API call failed: {}", url);
      throw new IOException("Pocket API call failed. See log.");
    }

    return new ByteArrayInputStream(resp.bodyAsBytes());
  }

  @SuppressWarnings( "unchecked" )
  private Map<String, String> combine( final Map<String, String> lhs,
      final Map<String, String> rhs )
  {
    if ( (lhs == null) || lhs.isEmpty() )
      return (Map<String, String>) ((rhs != null) ? rhs : ImmutableMap.of());
    if ( (rhs == null) || rhs.isEmpty() )
      return lhs;
    final TreeMap<String, String> ret = new TreeMap<String, String>();
    ret.putAll(rhs);
    ret.putAll(lhs);
    return ret;
  }

  @SuppressWarnings( "unused" )
  private Connection scrapeConnect( final String url )
  {
    final Connection conn = Conn.connect(url);
    for ( Map.Entry<String, String> entry : scrapeCookies.entrySet() )
      conn.cookie(entry.getKey(), entry.getValue());
    return conn;
  }

  private InputStream scrapeRequest( final String url,
      final Map<String, String> post_data ) throws IOException
  {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.trace("POST {} data: {}", url, post_data);

    final Connection conn = Conn.connect(url);
    conn.method(Connection.Method.POST);
    conn.ignoreHttpErrors(true);
    conn.ignoreContentType(true);
    conn.data(post_data);
    for ( Map.Entry<String, String> entry : scrapeCookies.entrySet() )
      conn.cookie(entry.getKey(), entry.getValue());
    final Response resp = conn.execute();
    log.trace("Response {}: {}", resp.statusCode(), resp.statusMessage());
    if ( resp.statusCode() != 200 )
    {
      for ( final Map.Entry<String, String> entry : resp.headers().entrySet() )
        log.trace("Header {}: {}", entry.getKey(), entry.getValue());
      log.error("Pocket scrape call failed: {}", url);
      throw new IOException("Pocket scrape call failed. See log.");
    }

    return new ByteArrayInputStream(resp.bodyAsBytes());
  }

  private void sendApiRequest( final ImmutableMap<String, Object> action )
    throws IOException
  {
    try
    {
      final String action_str = json.writeValueAsString(ImmutableList
          .of(action));
      final InputStream resp = apiRequest(API_BASE_URL + "/send",
        ImmutableMap.of("actions", action_str));
      final ActionResults results = readValue(resp, ActionResults.class);
      if ( !results.isSuccessful() )
        throw new IOException("Action failed: " + action.get("action"));
    }
    catch ( JsonProcessingException ex )
    {
      throw new UnhandledException(ex);
    }
  }

  private <T> T readValue( final InputStream in, final Class<T> cls )
    throws IOException
  {
    final ByteArrayInputOutputStream inout = new ByteArrayInputOutputStream();
    ByteStreams.copy(in, inout);
    {
      final StringWriter out = new StringWriter(inout.size());
      CharStreams.copy(new InputStreamReader(inout.getInputStream(),
          Charsets.UTF8), out);
      LoggerFactory.getLogger(getClass()).trace("Data returned: {}",
        out.toString());
    }
    return json.readValue(inout.getInputStream(), cls);
  }
}
