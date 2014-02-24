package com.evanmclean.erudite.pocket.json;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.evanmclean.evlib.lang.Str;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

class SubContent
{
  /**
   * If there are no images, then Pocket returns images as an empty string,
   * otherwise it returns it as a JSON object where each key is the image ID
   * (which we ignore) and the value is another JSON object (which we process
   * below).
   * 
   * @param obj
   * @return
   */
  private static ImmutableList<Image> getImages( final Object obj )
  {
    if ( obj == null )
      return ImmutableList.of();
    if ( obj.getClass().equals(String.class) )
    {
      if ( Str.isEmpty(obj.toString()) )
        return ImmutableList.of();
      throw new IllegalStateException("Images field returns string: " + obj);
    }

    final ImmutableList.Builder<Image> images = ImmutableList.builder();
    @SuppressWarnings( "unchecked" ) final Collection<Map<String, String>> maps = ((Map<Object, Map<String, String>>) obj)
        .values();
    for ( Map<String, String> map : maps )
    {
      final String image_id = map.get("image_id");
      final String src = map.get("src");
      if ( Str.isEmpty(image_id) || Str.isEmpty(src) )
        throw new IllegalStateException("Invalid image declaration: " + map);
      images.add(new Image(image_id, src));
    }
    return images.build();
  }

  private final String html;

  private final List<Image> images;

  SubContent( @JsonProperty( "article" ) final String html,
      @JsonProperty( "images" ) final Object images )
  {
    this.html = html;
    this.images = getImages(images);
  }

  @Override
  public String toString()
  {
    return "SubContent [html=" + html + ", images=" + images + "]";
  }

  String getHtml()
  {
    return html;
  }

  ImmutableList<Image> getImages()
  {
    return (ImmutableList<Image>) images;
  }
}
