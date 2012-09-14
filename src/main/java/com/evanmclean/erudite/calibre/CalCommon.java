package com.evanmclean.erudite.calibre;

import java.io.File;

import com.evanmclean.erudite.misc.Utils;
import com.evanmclean.evlib.io.Files;

/**
 * Some common routines used by the classes in this package.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
final class CalCommon
{
  static String findExe( final String exename )
  {
    File exe = Utils.findOnPath(exename);
    if ( exe == null )
    {
      if ( Utils.IS_WINDOWS )
        exe = Utils.findInFolder(exename, "C:\\Program Files\\Calibre2",
          "C:\\Program Files (x86)\\Calibre2");
      else
        exe = Utils.findInFolder(exename, "/usr", "/usr/local", "/opt/calibre",
          "/opt/calibre/bin");
    }
    if ( exe == null )
      throw new IllegalStateException("Could not find " + exename);
    return Files.getCanonicalPath(exe);
  }

  private CalCommon()
  {
    // empty
  }
}
