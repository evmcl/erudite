package com.evanmclean.erudite.readability.json;

import java.io.File;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Emmark
{
  public static void main( final String[] args )
  {
    try
    {
      final ObjectMapper json = new ObjectMapper();
      json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      final Bookmarks bm = json.readValue(new File(
          "/home/evan/projects/mcs/erudite/erudite/out.json"), Bookmarks.class);
      System.out.println(bm);
    }
    catch ( Exception ex )
    {
      ex.printStackTrace();
    }
    System.exit(0);
  }
}
