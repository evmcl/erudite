package com.evanmclean.erudite.pocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.pocket.Pocket.Filter;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.TreeSetIgnoreCase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Source object for Pocket.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
class PocketSource implements Source
{
  /**
   * Action to be used for onComplete or onError calls.
   */
  private static interface Action
  {
    void run( PocketArticle article ) throws IOException;
  }

  private static Filter makeFilter( final Config config )
  {
    final Boolean favourite;
    final ImmutableSet<String> included_tags;
    final ImmutableSet<String> excluded_tags;
    final ImmutableMap<String, String> post_data;
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

      final ImmutableMap.Builder<String, String> pdbldr = ImmutableMap
          .builder();
      if ( fav != null )
        pdbldr.put("favorite", fav.booleanValue() ? "1" : "0");
      if ( !inctags.isEmpty() )
        pdbldr.put("tag", inctags.first()); // Only include the first tag, not
                                            // sure if you can include multiple.

      favourite = fav;
      post_data = pdbldr.build();
      included_tags = ImmutableSortedSet.copyOfSorted(inctags);
      excluded_tags = exbldr.build();

      for ( final String tag : excluded_tags )
        if ( inctags.contains(tag) )
          throw new IllegalArgumentException("Specified filter of both " + tag
              + " and !" + tag + '.');
    }

    return new Filter() {
      @Override
      public Map<String, String> getPostData()
      {
        return post_data;
      }

      @Override
      public boolean isFiltered(
          final com.evanmclean.erudite.pocket.json.Article article )
      {
        if ( (favourite != null)
            && (article.isFavourite() != favourite.booleanValue()) )
          return true;

        final ImmutableSet<String> tags = article.getTags();

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
        public void run( final PocketArticle article ) throws IOException
        {
          article.archive();
        }
      };

    if ( Str.equalsOneOfIgnoreCase(str, "remove", "delete") )
      return new Action() {
        @Override
        public void run( final PocketArticle article ) throws IOException
        {
          article.remove();
        }
      };

    if ( Str.equalsOneOfIgnoreCase(str, "favourite", "favorite") )
      return new Action() {
        @Override
        public void run( final PocketArticle article ) throws IOException
        {
          article.favourite(true);
        }
      };

    if ( Str.equalsOneOfIgnoreCase(str, "unfavourite", "unfavorite") )
      return new Action() {
        @Override
        public void run( final PocketArticle article ) throws IOException
        {
          article.favourite(false);
        }
      };

    if ( Str.startsWithIgnoreCase(str, "tag:") )
    {
      final ImmutableList<String> tags = ImmutableList.copyOf(Str.split(
        Pattern.compile(","), str.substring(4), true, true));
      if ( tags.isEmpty() )
        throw new IllegalArgumentException("No tag specified.");
      return new Action() {
        @Override
        public void run( final PocketArticle article ) throws IOException
        {
          article.addTags(tags);
        }
      };
    }

    if ( Str.startsWithIgnoreCase(str, "untag:") )
    {
      final ImmutableList<String> tags = ImmutableList.copyOf(Str.split(
        Pattern.compile(","), str.substring(6), true, true));
      if ( tags.isEmpty() )
        throw new IllegalArgumentException("No tag specified.");
      return new Action() {
        @Override
        public void run( final PocketArticle article ) throws IOException
        {
          article.removeTags(tags);
        }
      };
    }

    if ( Str.equalsOneOf(str, "none", "nothing", Str.EMPTY, null) )
      return new Action() {
        @Override
        public void run( final PocketArticle article )
        {
          // no action
        }
      };

    throw new IllegalArgumentException("Unknown Pocket action: " + str);
  }

  private final Pocket pocket;
  private final Action onErrorAction;
  private final Action onCompleteAction;

  PocketSource( final Session session, final Config config )
  {
    pocket = new Pocket(session, makeFilter(config), config.getTitleMunger());
    onCompleteAction = parseAction(config.getString("on.complete"));
    onErrorAction = parseAction(config.getString("on.error"));
  }

  @Override
  public ImmutableList<? extends Article> getArticles() throws IOException
  {
    return pocket.getArticles();
  }

  @Override
  public String getName()
  {
    return "Pocket";
  }

  @Override
  public String getViaHtml()
  {
    return "<a href=\"" + Pocket.BASE_URL + "\">Pocket</a>";
  }

  @Override
  public void onComplete( final Article article ) throws IOException
  {
    onCompleteAction.run((PocketArticle) article);
  }

  @Override
  public void onError( final Article article ) throws IOException
  {
    onErrorAction.run((PocketArticle) article);
  }

}
