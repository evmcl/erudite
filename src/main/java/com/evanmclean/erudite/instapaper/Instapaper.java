package com.evanmclean.erudite.instapaper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.config.TitleMunger;
import com.evanmclean.erudite.misc.Conn;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.Colls;
import com.evanmclean.evlib.util.TreeMapIgnoreCase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Represents a login to the Instapaper service.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class Instapaper
{
  private class Article implements InstapaperArticle
  {
    private final String title;
    private final String sourceUrl;
    private final String summary;
    private final String textUrl;
    private final String archiveUrl;
    private final String moveUrl;
    private final String deleteUrl;
    private Element _text;

    Article( final String title, final String source_url, final String summary,
        final String text_url, final String archive_url, final String move_url,
        final String delete_url )
    {
      this.title = title;
      this.sourceUrl = source_url;
      this.summary = summary;
      this.textUrl = text_url;
      this.archiveUrl = archive_url;
      this.moveUrl = move_url;
      this.deleteUrl = delete_url;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void archive() throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Archiving article on Instapaper: {}", title);
      connect(archiveUrl).execute();
    }

    @Override
    public String getOriginalUrl()
    {
      return sourceUrl;
    }

    @Override
    public String getSourceUrl()
    {
      return "http://www.instapaper.com/text?u=" + Esc.url.text(sourceUrl);
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
    public void move( final InstapaperFolder folder ) throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Moving article on Instapaper to {} folder: {}", folder.getName(),
        title);
      final Folder fldr = (Folder) folder;
      connect(moveUrl + fldr.getId()).execute();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public void remove() throws IOException
    {
      LoggerFactory.getLogger(getClass()).trace(
        "Removing article on Instapaper: {}", title);
      connect(deleteUrl).execute();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public Element text() throws IOException
    {
      Element text = _text;
      if ( text == null )
      {
        LoggerFactory.getLogger(getClass()).trace(
          "Retrieving article from Instapaper: {}", title);
        final Document doc = connect(textUrl).get();
        final Element story = doc.getElementById("story");
        if ( story == null )
          throw new HasInstapaperLayoutChangedException(
              "Could not find div#story for article: " + title);

        final List<Node> contents = story.childNodes();
        switch ( contents.size() )
        {
          case 0:
            throw new HasInstapaperLayoutChangedException(
                "Empty div#story for article: " + title);
          case 1:
          {
            Node node = contents.get(0);
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
            for ( Node node : ImmutableList.copyOf(contents) )
              text.appendChild(node);
          }
        }

        _text = text;
      }

      return text.clone();
    }

    @Override
    public String toString()
    {
      final StringBuilder buff = new StringBuilder(title);

      buff.append("\n  ").append(sourceUrl) //
          .append("\n  Text: ").append(textUrl) //
          .append("\n  Archive: ").append(archiveUrl) //
          .append("\n  Move: ").append(moveUrl) //
          .append("\n  Delete: ").append(deleteUrl) //
      ;

      if ( Str.isNotEmpty(summary) )
        buff.append("\n\n  ").append(summary);
      return buff.toString();
    }
  }

  private class Folder implements InstapaperFolder
  {
    private final String name;
    private final String url;
    private final String id;
    private ImmutableList<InstapaperArticle> _articles;

    Folder( final String name, final String url, final String id )
    {
      this.name = name;
      this.url = url;
      this.id = id;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public ImmutableList<InstapaperArticle> getArticles() throws IOException
    {
      ImmutableList<InstapaperArticle> articles = _articles;
      if ( articles == null )
        _articles = articles = _saveArticles(connect(url).get());
      return articles;
    }

    @Override
    public String getName()
    {
      return name;
    }

    @Override
    public String toString()
    {
      return "InstapaperFolder(" + name + ", " + id + ", " + url + ')';
    }

    @SuppressWarnings( "synthetic-access" )
    ImmutableList<InstapaperArticle> _saveArticles( final Document doc )
    {
      final Logger log = LoggerFactory.getLogger(getClass());
      log.trace("Retrieving article list from Instapaper for folder {}.", name);

      final ImmutableList.Builder<InstapaperArticle> bldr = ImmutableList
          .builder();

      final Element articles = doc.getElementById("bookmark_list");
      if ( articles != null )
      {
        for ( Element article : articles.children() )
          if ( "div".equalsIgnoreCase(article.tagName()) )
          {
            final String title;
            final String source_url;
            final String summary;
            {
              final Element container = article.getElementsByClass("titleRow")
                  .first();
              if ( container == null )
                continue;
              {
                final Element link = container.getElementsByTag("a").first();
                if ( link == null )
                  continue;
                title = titleMunger.munge(link.text());
                source_url = link.attr("href");
                if ( Str.isEmpty(title) || Str.isEmpty(source_url) )
                  continue;
              }
              {
                final Element smry = container.getElementsByClass("summary")
                    .first();
                summary = (smry == null) ? Str.EMPTY : Str.trimToEmpty(smry
                    .text());
              }
            }

            final String text_url;
            final String archive_url;
            {
              final Element container = article.getElementsByClass(
                "cornerControls").first();
              if ( container == null )
                throw new HasInstapaperLayoutChangedException(
                    "Cannot find div.cornerControls for article: " + title);

              String tu = null;
              String au = null;

              for ( Element link : container.getElementsByTag("a") )
              {
                final String text = link.text();
                if ( "text".equalsIgnoreCase(text) )
                  tu = link.attr("href");
                else if ( "archive".equalsIgnoreCase(text) )
                  au = link.attr("href");
              }

              if ( Str.isEmpty(tu) )
                throw new HasInstapaperLayoutChangedException(
                    "Cannot find text url for article: " + title);

              if ( Str.isEmpty(au) )
                throw new HasInstapaperLayoutChangedException(
                    "Cannot find archive url for article: " + title);

              text_url = BASE_URL + tu;
              archive_url = BASE_URL + au;
            }

            final String delete_url;
            final String move_url;
            {
              final Element container = article.getElementsByClass(
                "secondaryControls").first();
              if ( container == null )
                throw new HasInstapaperLayoutChangedException(
                    "Cannot find div.secondaryControls for article: " + title);

              String du = null;

              for ( Element link : container.getElementsByTag("a") )
              {
                final String text = link.text();
                if ( "delete".equalsIgnoreCase(text) )
                  du = link.attr("href");
              }

              if ( Str.isEmpty(du) || (du == null) )
                throw new HasInstapaperLayoutChangedException(
                    "Cannot find delete url for article: " + title);

              delete_url = BASE_URL + du;

              // This is a bit of a kludge: We just generate the fragment of the
              // URL for moving to a folder.
              move_url = BASE_URL + du.replace("delete", "move") + "/to/";
            }

            log.trace("Article: {}", title);
            bldr.add(new Article(title, source_url, summary, text_url,
                archive_url, move_url, delete_url));
          }
      }

      final ImmutableList<InstapaperArticle> list = bldr.build();
      return list;
    }

    String getId()
    {
      return id;
    }
  }

  /**
   * The name of the default (Read Later) folder on Instapaper.
   */
  public static final String DEFAULT_FOLDER = "Read Later";

  private static final String BASE_URL = "https://www.instapaper.com";
  private static final String DEFAULT_URL = BASE_URL + "/u";
  private static final String DEFAULT_ID = "0";

  /**
   * Create a {@link Session} for a login to the Instapaper service. Password is
   * not stored by the application, only session cookies.
   * 
   * @param email
   *        The user's email address.
   * @param pass
   *        The user's password.
   * @return A session that can be used to instantiate an {@link Instapaper}
   *         object or <code>null</code> if login failed.
   * @throws IOException
   */
  public static Session login( final String email, final String pass )
    throws IOException
  {
    final Connection conn = Conn.connect(BASE_URL + "/user/login");
    conn.data("username", email, "password", pass);
    conn.method(Connection.Method.POST);

    Connection.Response resp = conn.execute();
    if ( resp.cookies().isEmpty() )
      return null;
    return new InstapaperSession(ImmutableMap.copyOf(resp.cookies()));
  }

  private final InstapaperSession session;
  private final TitleMunger titleMunger;

  private ImmutableMap<String, Folder> _folders;

  /**
   * Create a logged-in connection to Instapaper based on a session previously
   * produced by {@link #login(String, String)}.
   * 
   * @param session
   *        The session object to use.
   * @param title_munger
   *        A title munger to use on article titles.
   */
  public Instapaper( final Session session, final TitleMunger title_munger )
  {
    if ( !(session instanceof InstapaperSession) )
      throw new IllegalArgumentException(
          "Invalid session object for Instapaper.");

    this.session = (InstapaperSession) session;
    this.titleMunger = (title_munger != null) ? title_munger : TitleMunger
        .empty();
  }

  /**
   * Get a folder from Instapaper.
   * 
   * @param name
   *        The name of the folder.
   * @return The folder, or <code>null</code> if it does not exist.
   * @throws IOException
   */
  public InstapaperFolder getFolder( final String name ) throws IOException
  {
    return _getFolders().get(name);
  }

  /**
   * A list of all the folders on Instapaper.
   * 
   * @return A list of all the folders on Instapaper.
   * @throws IOException
   */
  public ImmutableList<InstapaperFolder> getFolders() throws IOException
  {
    return ImmutableList.<InstapaperFolder> copyOf(_getFolders().values());
  }

  /**
   * Get the default folder (equivalent to
   * <code>getFolder(DEFAULT_FOLDER)</code>.)
   * 
   * @return The default folder.
   * @throws IOException
   */
  public InstapaperFolder getReadLaterFolder() throws IOException
  {
    return _getFolders().get(DEFAULT_FOLDER);
  }

  private ImmutableMap<String, Folder> _getFolders() throws IOException
  {
    ImmutableMap<String, Folder> folders = _folders;
    if ( folders == null )
    {
      final Logger log = LoggerFactory.getLogger(getClass());
      log.trace("Retrieving folder list from Instapaper.");
      final Document doc = connect(DEFAULT_URL).get();
      final TreeMapIgnoreCase<Folder> map = Colls.newTreeMapIgnoreCase();
      {
        final Folder folder = new Folder(DEFAULT_FOLDER, DEFAULT_URL,
            DEFAULT_ID);
        folder._saveArticles(doc);
        log.trace("Folder: {}", DEFAULT_FOLDER);
        map.put(folder.getName(), folder);
      }

      final Element container = doc.getElementById("folders");
      if ( container != null )
      {
        final Pattern idpat = Pattern.compile("\\/u\\/folder\\/(\\d+)\\/");
        for ( Element el : container.children() )
          if ( "div".equalsIgnoreCase(el.tagName()) )
          {
            final Element link = el.getElementsByTag("a").first();
            if ( link != null )
            {
              final String name = Str.trimToNull(link.text());
              final String url = link.attr("href");
              final String id;
              {
                final Matcher mat = idpat.matcher(url);
                if ( mat.find() )
                  id = mat.group(1);
                else
                  id = null;
              }

              if ( Str.isNotEmpty(name) && Str.isNotEmpty(url)
                  && Str.isNotEmpty(id) )
              {
                log.trace("Folder: {}", name);
                map.put(name, new Folder(name, BASE_URL + url, id));
              }
            }
          }
      }

      _folders = folders = ImmutableSortedMap.copyOfSorted(map);
    }
    return folders;
  }

  private Connection connect( final String url )
  {
    final Connection conn = Conn.connect(url);
    for ( Map.Entry<String, String> entry : session.getCookies().entrySet() )
      conn.cookie(entry.getKey(), entry.getValue());
    return conn;
  }
}
