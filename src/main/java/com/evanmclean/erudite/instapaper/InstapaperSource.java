package com.evanmclean.erudite.instapaper;

import java.io.IOException;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Articles;
import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.evlib.lang.Str;

/**
 * Source object for Instapaper.
 *
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class InstapaperSource implements Source
{
  /**
   * Action to be used for onComplete or onError calls.
   */
  private static interface Action
  {
    void run( InstapaperArticle article ) throws IOException;
  }

  /**
   * Create the action based on a string from the configuration file. See
   * example configuration file in the user documentation.
   *
   * @param ip
   * @param str
   * @return
   */
  private static Action parseAction( final Instapaper ip, final String str )
  {
    if ( "archive".equalsIgnoreCase(str) )
      return new Action() {
        @Override
        public void run( final InstapaperArticle article ) throws IOException
        {
          article.archive();
        }
      };

    if ( Str.equalsOneOfIgnoreCase(str, "remove", "delete") )
      return new Action() {
        @Override
        public void run( final InstapaperArticle article ) throws IOException
        {
          article.remove();
        }
      };

    if ( Str.startsWithIgnoreCase(str, "move:") )
    {
      final String move_folder = Str.trimToNull(str.substring(5));
      if ( move_folder == null )
        throw new IllegalArgumentException(
            "No Instapaper folder specified for moving article.");
      return new Action() {
        @Override
        public void run( final InstapaperArticle article ) throws IOException
        {
          final InstapaperFolder folder = ip.getFolder(move_folder);
          if ( folder == null )
            throw new IOException("Unknown Instapaper folder: " + move_folder);
          article.move(folder);
        }
      };
    }

    if ( Str.equalsOneOf(str, "none", "nothing", Str.EMPTY, null) )
      return new Action() {
        @Override
        public void run( final InstapaperArticle article )
        {
          // no action
        }
      };

    throw new IllegalArgumentException("Unknown Instapaper action: " + str);
  }

  private final Instapaper ip;
  private final String articleFolder;
  private final Action onErrorAction;
  private final Action onCompleteAction;

  InstapaperSource( final Session session, final Config config )
  {
    ip = new Instapaper(session, config.getTitleMunger());
    articleFolder = config.getString("folder");
    onCompleteAction = parseAction(ip, config.getString("on.complete"));
    onErrorAction = parseAction(ip, config.getString("on.error"));
  }

  @Override
  public Articles getArticles() throws IOException
  {
    final InstapaperFolder folder = (articleFolder == null) ? ip
        .getReadLaterFolder() : ip.getFolder(articleFolder);
    if ( folder == null )
      throw new IOException("Unknown Instapaper folder: "
          + Str.ifNull(articleFolder, Instapaper.DEFAULT_FOLDER));
    return new Articles(folder.getArticles());
  }

  @Override
  public String getName()
  {
    return "Instapaper";
  }

  @Override
  public String getViaHtml()
  {
    return "<a href=\"https://www.instapaper.com/\">Instapaper</a>";
  }

  @Override
  public void onComplete( final Article article ) throws IOException
  {
    onCompleteAction.run((InstapaperArticle) article);
  }

  @Override
  public void onError( final Article article ) throws IOException
  {
    onErrorAction.run((InstapaperArticle) article);
  }

}
