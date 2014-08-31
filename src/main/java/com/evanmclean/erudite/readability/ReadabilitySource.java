package com.evanmclean.erudite.readability;

import java.io.IOException;
import java.util.List;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Articles;
import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.readability.Readability.Filter;
import com.evanmclean.erudite.readability.json.Bookmark;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.evlib.escape.Esc;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.TreeSetIgnoreCase;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Source object for Readability.
 *
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class ReadabilitySource implements Source
{
  /**
   * Action to be used for onComplete or onError calls.
   */
  private static interface Action
  {
    void run( ReadabilityArticle article ) throws IOException;
  }

  private static Filter makeFilter( final Config config )
  {
    final Boolean favourite;
    final ImmutableSet<String> included_tags;
    final ImmutableSet<String> excluded_tags;
    final String api_args;
    {
      Boolean fav = null;
      final TreeSetIgnoreCase inctags = new TreeSetIgnoreCase();
      final ImmutableSet.Builder<String> exbldr = ImmutableSet.builder();

      final List<String> filters = config.getStrings("filter");
      for ( final String filter_line : filters )
        for ( String filter : filter_line.split("\\s*,\\s*") )
        {
          filter = Str.trimToEmpty(filter);
          final boolean not = filter.startsWith("!");
          if ( not )
            filter = Str.trimToEmpty(filter.substring(1));
          if ( Str.equalsOneOfIgnoreCase(filter, "favourite", "favorite") )
          {
            if ( (fav != null) && (fav.booleanValue() == not) )
              throw new IllegalArgumentException(
                  "Specified filter of both favourite and !favourite.");
            fav = !not;
          }
          else if ( Str.isNotEmpty(filter) )
          {
            if ( not )
              exbldr.add(filter);
            else
              inctags.add(filter);
          }
        }

      final StringBuilder str = new StringBuilder();

      if ( !inctags.isEmpty() )
      {
        if ( str.length() > 0 )
          str.append('&');
        str.append("tags=");
        boolean first = true;
        for ( final String tag : inctags )
        {
          if ( first )
            first = false;
          else
            str.append("%2C"); // Comma, URL encoded.
          str.append(Esc.url.text(tag));
        }
      }

      favourite = fav;
      api_args = str.toString();
      included_tags = ImmutableSortedSet.copyOfSorted(inctags);
      excluded_tags = exbldr.build();

      for ( final String tag : excluded_tags )
        if ( inctags.contains(tag) )
          throw new IllegalArgumentException("Specified filter of both " + tag
            + " and !" + tag + '.');
    }

    return new Filter() {
      @Override
      public String getApiArgs()
      {
        return api_args;
      }

      @Override
      public boolean isFiltered( final Bookmark bookmark )
      {
        if ( (favourite != null)
            && (bookmark.isFavourite() != favourite.booleanValue()) )
          return true;

        final ImmutableSet<String> tags = bookmark.getTags().keySet();

        for ( final String tag : included_tags )
          if ( !tags.contains(tag) )
            return true;

        for ( final String tag : excluded_tags )
          if ( tags.contains(tag) )
            return true;

        return false;
      }
    };
  }

  /**
   * Create the action based on a string from the configuration file. See
   * example configuration file in the user documentation.
   *
   * @param str
   * @return
   */
  private static Action parseAction( final String str )
  {
    if ( "archive".equalsIgnoreCase(str) )
      return new Action() {
      @Override
      public void run( final ReadabilityArticle article ) throws IOException
      {
        article.archive();
      }
    };

    if ( Str.equalsOneOfIgnoreCase(str, "remove", "delete") )
      return new Action() {
      @Override
      public void run( final ReadabilityArticle article ) throws IOException
      {
        article.remove();
      }
    };

    if ( Str.equalsOneOfIgnoreCase(str, "favourite", "favorite") )
      return new Action() {
      @Override
      public void run( final ReadabilityArticle article ) throws IOException
      {
        article.favourite(true);
      }
    };

    if ( Str.equalsOneOfIgnoreCase(str, "unfavourite", "unfavorite") )
      return new Action() {
      @Override
      public void run( final ReadabilityArticle article ) throws IOException
      {
        article.favourite(false);
      }
    };

    if ( Str.startsWithIgnoreCase(str, "tag:") )
    {
      final String tag = Str.trimToNull(str.substring(4));
      if ( tag == null )
        throw new IllegalArgumentException("No tag specified.");
      return new Action() {
        @Override
        public void run( final ReadabilityArticle article ) throws IOException
        {
          article.addTags(tag);
        }
      };
    }

    if ( Str.startsWithIgnoreCase(str, "untag:") )
    {
      final String tag = Str.trimToNull(str.substring(4));
      if ( tag == null )
        throw new IllegalArgumentException("No tag specified.");
      return new Action() {
        @Override
        public void run( final ReadabilityArticle article ) throws IOException
        {
          article.removeTag(tag);
        }
      };
    }

    if ( Str.equalsOneOf(str, "none", "nothing", Str.EMPTY, null) )
      return new Action() {
      @Override
      public void run( final ReadabilityArticle article )
      {
        // no action
      }
    };

    throw new IllegalArgumentException("Unknown Readability action: " + str);
  }

  private final Readability rd;
  private final Action onErrorAction;
  private final Action onCompleteAction;

  ReadabilitySource( final Session session, final Config config )
  {
    rd = new Readability(session, makeFilter(config), config.getTitleMunger());
    onCompleteAction = parseAction(config.getString("on.complete"));
    onErrorAction = parseAction(config.getString("on.error"));
  }

  @Override
  public Articles getArticles() throws IOException
  {
    return new Articles(rd.getArticles());
  }

  @Override
  public String getName()
  {
    return "Readability";
  }

  @Override
  public String getViaHtml()
  {
    return "<a href=\"http://www.readability.com/\">Readability</a>";
  }

  @Override
  public void onComplete( final Article article ) throws IOException
  {
    onCompleteAction.run((ReadabilityArticle) article);
  }

  @Override
  public void onError( final Article article ) throws IOException
  {
    onErrorAction.run((ReadabilityArticle) article);
  }

}
