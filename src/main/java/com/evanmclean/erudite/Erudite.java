package com.evanmclean.erudite;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.misc.Utils;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.Colls;
import com.google.common.collect.ImmutableList;

/**
 * Creates the full HTML document based on the article, complete with footnotes
 * and images.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class Erudite
{
  private static class Footnote
  {
    private final String url;
    private final String num;
    private final String text;

    Footnote( final String url, final int num, final String text )
    {
      this(url, String.valueOf(num), text);
    }

    Footnote( final String url, final String num, final String text )
    {
      this.url = url;
      this.num = num;
      this.text = text;
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
      Footnote other = (Footnote) obj;
      if ( num == null )
      {
        if ( other.num != null )
          return false;
      }
      else if ( !num.equals(other.num) )
        return false;
      if ( text == null )
      {
        if ( other.text != null )
          return false;
      }
      else if ( !text.equals(other.text) )
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
      result = prime * result + ((num == null) ? 0 : num.hashCode());
      result = prime * result + ((text == null) ? 0 : text.hashCode());
      result = prime * result + ((url == null) ? 0 : url.hashCode());
      return result;
    }

    String getNum()
    {
      return num;
    }

    String getText()
    {
      return text;
    }

    String getUrl()
    {
      return url;
    }
  }

  private final Logger log = LoggerFactory.getLogger(Erudite.class);

  /**
   * Process an article and produce a complete HTML document. This object is
   * thread-safe.
   * 
   * @param article
   *        The article to process.
   * @param source
   *        The source of the article.
   * @param template
   *        The template used to produce the full HTML document.
   * @param image_handler
   *        Used for downloading any images we find.
   * @param do_footnotes
   *        Should we produce footnotes for links?
   * @param hacker_news_urls
   *        The hacker news discussion URLs for the article.
   * @return The full HTML document representing the article.
   * @throws IOException
   */
  public Document process( final Article article, final Source source,
      final Template template, final ImageHandler image_handler,
      final boolean do_footnotes, final ImmutableList<String> hacker_news_urls )
    throws IOException
  {
    log.debug("Processing {}", article.getTitle());
    final Document doc = template.getDocument();
    doc.title(article.getTitle());
    setTextElementByClass(doc, "erudite_title", article.getTitle());
    setAByClass(doc, "erudite_original_url", article.getOriginalUrl());
    setAByClass(doc, "erudite_original_link", article.getOriginalUrl(), null);
    insertPlug(doc, source);

    insertSourceMeta(doc, article, source);
    insertHackerNewsMeta(doc, hacker_news_urls);

    final Element content = article.text();
    if ( do_footnotes )
      insertFootnotes(doc, content, article.getOriginalUrl());
    else
      removeFootnotes(doc);

    handleImages(content, image_handler);

    insertContent(doc, content);
    return doc;
  }

  private void handleImages( final Element content,
      final ImageHandler image_handler ) throws IOException
  {
    final Map<String, String> mappings = Colls.newHashMap();
    final List<Element> yank = Colls.newArrayList();

    final Elements images = content.getElementsByTag("img");
    if ( images != null )
      for ( Element image : images )
      {
        final String orig_source = image.attr("src");
        if ( Str.isEmpty(orig_source) )
        {
          yank.add(image);
        }
        else if ( image_handler == null )
        {
          log.debug("No image handler, so removing image from the HTML: {}",
            orig_source);
          yank.add(image);
        }
        else
        {
          String new_source = mappings.get(orig_source);
          if ( new_source == null )
          {
            new_source = image_handler.image(orig_source);
            if ( new_source != null )
              mappings.put(orig_source, new_source);
          }
          if ( new_source != null )
          {
            log.debug("Processed image from {} => {}", orig_source, new_source);
            image.attr("src", new_source);
          }
          else
          {
            log.debug(
              "Could not retrieve image, so removing from the HTML: {}",
              orig_source);
            yank.add(image);
          }
        }
      }

    for ( Element image : yank )
      image.remove();
  }

  private void insertContent( final Document doc, final Element element )
  {
    final Element content = doc.getElementById("erudite_contents");
    content.empty();
    content.appendChild(element);
  }

  private void insertFootnotes( final Document doc, final Element content,
      final String src_url )
  {
    final Element footnotes_element = doc.getElementById("erudite_footnotes");
    if ( footnotes_element == null )
    {
      log.debug("No footnotes element in template!");
      removeFootnotes(doc);
      return;
    }

    int next_footnote_num = 1;
    final List<Footnote> footnotes_list = Colls.newArrayList();
    final Map<String, Footnote> footnotes_map = Colls.newHashMap();
    final Tag SUP_TAG = Tag.valueOf("sup");
    final Tag LINK_TAG = Tag.valueOf("a");

    final Elements links = content.getElementsByTag("a");
    if ( links != null )
      for ( Element link : links )
      {
        final String href = link.attr("href");
        if ( Str.isNotEmpty(href) //
            && (href.charAt(0) != '#') //
            && Str.notEquals(href, src_url) )
        {
          final String text = Str.trimToEmpty(link.text());
          if ( !isSameUrl(href, text) )
          {
            boolean first = false;
            Footnote footnote = footnotes_map.get(href);
            if ( footnote == null )
            {
              footnote = new Footnote(href, next_footnote_num++, text);
              footnotes_list.add(footnote);
              footnotes_map.put(href, footnote);
              first = true;
            }
            else if ( footnote.getText().isEmpty() && (!text.isEmpty()) )
            {
              // If the original footnote didn't have any text, replace with
              // one that did.
              final int pos = footnotes_list.indexOf(footnote);
              footnote = new Footnote(href, footnote.getNum(), text);
              footnotes_list.set(pos, footnote);
              footnotes_map.put(href, footnote);
            }

            // Create insertion.
            {
              final Element tag = new Element(LINK_TAG, Str.EMPTY);
              tag.addClass("erudite_footnote_source");
              tag.attr("href", "#erudite_footnote_" + footnote.getNum());
              tag.attr("title", "Goto footnote");
              if ( first )
                tag.attr("id", "erudite_sourcenote_" + footnote.getNum());
              {
                final Element sup = new Element(SUP_TAG, Str.EMPTY);
                sup.text("[" + footnote.getNum() + ']');
                tag.appendChild(sup);
              }
              link.after(tag);
              link.after("&nbsp;");
            }
          }
        }
      }

    if ( footnotes_list.isEmpty() )
    {
      log.debug("No footnotes for this article.");
      removeFootnotes(doc);
      return;
    }

    // Create list of footnotes.
    final Tag DT_TAG = Tag.valueOf("dt");
    final Tag DD_TAG = Tag.valueOf("dd");
    for ( Footnote footnote : footnotes_list )
    {
      final String text = Utils.abbreviate(footnote.getText(), 40);

      {
        final Element dt = new Element(DT_TAG, Str.EMPTY);
        dt.attr("id", "erudite_footnote_" + footnote.getNum());
        footnotes_element.appendChild(dt);

        {
          final Element src_link = new Element(LINK_TAG, Str.EMPTY);
          src_link.attr("href", "#erudite_sourcenote_" + footnote.getNum());
          if ( Str.isNotEmpty(text) )
            src_link.attr("title", text);
          src_link.text("[" + footnote.getNum() + ']');
          dt.appendChild(src_link);
        }

        dt.appendText(" ");
        dt.appendText(text);
      }

      {
        final Element dd = new Element(DD_TAG, Str.EMPTY);
        footnotes_element.appendChild(dd);
        final Element link = new Element(LINK_TAG, Str.EMPTY);
        link.attr("href", footnote.getUrl());
        link.attr("class", "erudite_url_url");
        if ( Str.isNotEmpty(text) )
          link.attr("title", text);
        link.text(footnote.getUrl());
        dd.appendChild(link);
      }
    }

    if ( footnotes_list.size() == 1 )
      log.debug("Inserted one footnote.");
    else
      log.debug("Inserted {} footnotes.", footnotes_list.size());
  }

  private void insertHackerNewsMeta( final Document doc,
      final ImmutableList<String> hacker_news_urls )
  {
    // #### No hacker news links.
    if ( (hacker_news_urls == null) || hacker_news_urls.isEmpty() )
    {
      removeByClass(doc, "erudite_hn_info");
      removeByClass(doc, "erudite_hn_infos");
      return;
    }

    // #### One hacker news link.
    if ( hacker_news_urls.size() == 1 )
    {
      removeByClass(doc, "erudite_hn_infos");
      final String url = hacker_news_urls.get(0);
      setAByClass(doc, "erudite_hn_url", url);
      setAByClass(doc, "erudite_hn_link", url, null);
      return;
    }

    // #### Several hacker news links.
    removeByClass(doc, "erudite_hn_info");

    final Elements links = doc.getElementsByClass("erudite_hn_links");
    for ( final Element el : links )
      el.empty();

    final Elements urls = doc.getElementsByClass("erudite_hn_urls");
    for ( final Element el : urls )
      el.empty();

    int link_num = 0;
    boolean first = true;
    for ( final String hn_url : hacker_news_urls )
    {
      for ( final Element el : links )
      {
        if ( !first )
          el.appendText(", ");
        el.appendElement("a").attr("class", "erudite_url_link erudite_hn_link")
            .attr("href", hn_url).text("#" + String.valueOf(++link_num));
      }

      for ( final Element el : urls )
      {
        if ( !first )
          el.appendText(", ");
        el.appendElement("a").attr("class", "erudite_url_url erudite_hn_url")
            .attr("href", hn_url).text(hn_url);
      }
      first = false;
    }
  }

  private int insertPlug( final Document doc, final Source source )
  {
    final Elements plugs = doc.getElementsByClass("erudite_source");
    if ( (plugs == null) || (plugs.size() <= 0) )
    {
      log.debug("Template missing source plug element.");
      return 0;
    }
    for ( final Element plug : plugs )
    {
      plug.empty();
      plug.html(source.getViaHtml());
    }
    return plugs.size();
  }

  private void insertSourceMeta( final Document doc, final Article article,
      final Source source )
  {
    final String source_url = article.getSourceUrl();
    if ( Str.isEmpty(source_url) )
    {
      removeByClass(doc, "erudite_source_info");
    }
    else
    {
      setAByClass(doc, "erudite_source_url", source_url);
      setAByClass(doc, "erudite_source_link", source_url, null);
      setTextElementByClass(doc, "erudite_source_name", source.getName());
    }
  }

  private boolean isSameUrl( final String href, final String text )
  {
    if ( href.equals(text) )
      return true;
    if ( !text.startsWith("http://") )
      if ( href.equals("http://" + text) )
        return true;
    return false;
  }

  private int removeByClass( final Document doc, final String clsname )
  {
    final Elements elements = doc.getElementsByClass(clsname);
    if ( elements == null )
      return 0;
    final int sz = elements.size();
    for ( final Element element : ImmutableList.copyOf(elements) )
    {
      element.empty();
      element.remove();
    }
    return sz;
  }

  private boolean removeById( final Document doc, final String id )
  {
    Element element = doc.getElementById(id);
    if ( element == null )
      return false;
    element.empty();
    element.remove();
    return true;
  }

  private void removeFootnotes( final Document doc )
  {
    if ( !removeById(doc, "erudite_footnotes_section") )
      removeById(doc, "erudite_footnotes");
  }

  private int setAByClass( final Document doc, final String clsname,
      final String href )
  {
    return setAByClass(doc, clsname, href, href);
  }

  private int setAByClass( final Document doc, final String clsname,
      final String href, final String text )
  {
    final Elements elements = doc.getElementsByClass(clsname);
    if ( elements == null )
      return 0;
    for ( final Element element : elements )
    {
      element.attr("href", href);
      if ( text != null )
      {
        element.empty();
        element.text(text);
      }
    }
    return elements.size();
  }

  @SuppressWarnings( "unused" )
  private boolean setAById( final Document doc, final String id,
      final String href )
  {
    return setAById(doc, id, href, href);
  }

  private boolean setAById( final Document doc, final String id,
      final String href, final String text )
  {
    final Element element = doc.getElementById(id);
    if ( element == null )
      return false;
    element.attr("href", href);
    element.empty();
    element.text(text);
    return true;
  }

  private int setTextElementByClass( final Document doc, final String clsname,
      final String text )
  {
    final Elements elements = doc.getElementsByClass(clsname);
    if ( elements == null )
      return 0;
    for ( final Element element : elements )
    {
      element.empty();
      element.text(text);
    }
    return elements.size();
  }

  @SuppressWarnings( "unused" )
  private boolean setTextElementById( final Document doc, final String id,
      final String text )
  {
    final Element element = doc.getElementById(id);
    if ( element == null )
      return false;
    element.empty();
    element.text(text);
    return true;
  }
}
