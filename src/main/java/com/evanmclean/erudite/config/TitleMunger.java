package com.evanmclean.erudite.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.misc.Conv;
import com.google.common.collect.ImmutableList;

/**
 * Performs munging on the title of an article. Can be used to convert the title
 * of the article (i.e., the HTML document) to something more appropriate for
 * your e&ndash;book library. See the sample configuration in the user
 * documentation for examples.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class TitleMunger
{
  /**
   * Will build a title munger out of a set of <code>/regex/str/</code> regular
   * expression substitutions. First match wins.
   * 
   * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
   *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
   */
  public static class Builder
  {
    final ImmutableList.Builder<Munger> mungers = ImmutableList.builder();

    /**
     * Add a <code>/regex/str/</code> regular expression substitution to be used
     * by the title munger. The substition can include arguments &ldquo;
     * <code>\1</code>&rdquo; through &ldquo;<code>\9</code>&rdquo; to
     * substitute values out of the regular expression.
     * 
     * @param str
     * @return The builder object.
     */
    public Builder add( final String str )
    {
      mungers.add(parse(str));
      return this;
    }

    /**
     * Build the title munger.
     * 
     * @return The title munger.
     */
    @SuppressWarnings( "synthetic-access" )
    public TitleMunger build()
    {
      return new TitleMunger(mungers.build());
    }

    private ImmutableList<Element> makerep( final String str )
    {
      final Pattern sub = Pattern.compile("\\\\(\\d)");
      final Matcher mat = sub.matcher(str);
      final ImmutableList.Builder<Element> bldr = ImmutableList.builder();
      int start = 0;
      while ( mat.find(start) )
      {
        final int end = mat.start();
        if ( end > start )
          bldr.add(new StaticElement(str.substring(start, end)));
        start = mat.end();
        final int idx = Conv.toInt(mat.group(1), -1);
        bldr.add(new DynamicElement(idx));
      }

      final int len = str.length();
      if ( len > start )
        bldr.add(new StaticElement(str.substring(start)));

      return bldr.build();
    }

    private Munger parse( final String str )
    {
      final Pattern regex = Pattern.compile("/(.+)/([^/]+)/");
      final Matcher mat = regex.matcher(str);
      if ( !mat.matches() )
        throw new IllegalArgumentException(
            "Expect regular expression in the form /regex/replace/: " + regex);
      return new Munger(Pattern.compile(mat.group(1), Pattern.CASE_INSENSITIVE
          | Pattern.UNICODE_CASE | Pattern.CANON_EQ), makerep(mat.group(2)));
    }

  }

  private static class DynamicElement implements Element
  {
    private final int num;

    DynamicElement( final int num )
    {
      this.num = num;
    }

    @Override
    public String str( final Matcher mat )
    {
      return mat.group(num);
    }
  }

  private static interface Element
  {
    String str( Matcher mat );
  }

  private static class Munger
  {
    private final Pattern pat;
    private final ImmutableList<Element> rep;

    Munger( final Pattern pat, final ImmutableList<Element> rep )
    {
      this.pat = pat;
      this.rep = rep;
    }

    /**
     * Perform the substitution operation on the string. Returns null if the
     * regular expression does not match or the result would be a blank string.
     * 
     * @param str
     *        String to perform the substitution on.
     * @return The substituted string, or null if the regular expression does
     *         not match or it would have been a blank string. Result is trimmed
     *         of leading and trailing white space.
     */
    String munge( final String str )
    {
      final Matcher mat = pat.matcher(str);
      if ( !mat.matches() )
        return null;
      final StringBuilder buff = new StringBuilder(str.length());
      for ( Element el : rep )
        buff.append(el.str(mat));
      return Str.trimToNull(buff.toString());
    }
  }

  private static class StaticElement implements Element
  {
    private final String str;

    StaticElement( final String str )
    {
      this.str = str;
    }

    @Override
    public String str( final Matcher mat )
    {
      return str;
    }
  }

  /**
   * Create a title munger builder.
   * 
   * @return A title munger builder.
   */
  public static Builder builder()
  {
    return new Builder();
  }

  /**
   * Create a title munger that does no munging.
   * 
   * @return A tile munger that does no munging.
   */
  public static TitleMunger empty()
  {
    final ImmutableList<Munger> none = ImmutableList.of();
    return new TitleMunger(none);
  }

  private final ImmutableList<Munger> mungers;

  private TitleMunger( final ImmutableList<Munger> mungers )
  {
    this.mungers = mungers;
  }

  /**
   * Perform the substitution operation on the string.
   * 
   * @param str
   *        String to perform the substitution on.
   * @return The substituted string, or <code>str</code> trimmed of any leading
   *         and trailing white space if there was no substitution.
   */
  public String munge( String str )
  {
    str = Str.trimToNull(str);
    if ( str == null )
      return Str.EMPTY;
    for ( Munger munger : mungers )
    {
      final String munged = munger.munge(str);
      if ( munged != null )
        return munged;
    }
    return str;
  }
}
