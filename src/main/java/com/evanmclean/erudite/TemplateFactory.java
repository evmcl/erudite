package com.evanmclean.erudite;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.cli.Args;
import com.evanmclean.evlib.io.Files;
import com.evanmclean.evlib.lang.Str;
import com.google.common.io.ByteSource;

/**
 * Loads {@link Template}s.
 *
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class TemplateFactory
{
  private static final String DEFAULT_TEMPLATE_KEY = "__erudite_default_template_key";

  private final ConcurrentHashMap<String, Template> templates = new ConcurrentHashMap<String, Template>();
  private final ByteSource defaultTemplateSupplier;
  private final Object[] lock = new Object[0];

  /**
   * Construct the template factory.
   *
   * @param default_template
   *        Input supplier for loading the default template (if needed).
   * @see #getDefault()
   */
  public TemplateFactory( final ByteSource default_template )
  {
    this.defaultTemplateSupplier = default_template;
  }

  /**
   * Loads the {@link Template} from the specified HTML file. If
   * <code>file</code> is <code>null</code> then same as calling
   * {@link #getDefault()}.
   *
   * @param file
   * @return The template.
   * @throws IOException
   */
  public Template get( final File file ) throws IOException
  {
    if ( file == null )
      return getDefault();
    final String path = Files.getCanonicalPath(file);
    Template template = templates.get(path);
    if ( template == null )
      synchronized ( lock )
      {
        template = templates.get(path);
        if ( template == null )
        {
          template = new Template(file);
          templates.put(path, template);
          LoggerFactory.getLogger(getClass()).trace("Loaded template from: {}",
            file);
        }
      }
    return template;
  }

  /**
   * Loads the {@link Template} from the specified HTML file. If
   * <code>filename</code> is an empty string or <code>null</code> then same as
   * calling {@link #getDefault()}. If the string contains just the name of a
   * file (no folder info) it will look for the file in the current directory
   * and failing that, the erudite application folder (e.g.,
   * <code>~/.erudite</code>).
   *
   * @param filename
   * @return The template.
   * @throws IOException
   */
  public Template get( final String filename ) throws IOException
  {
    if ( Str.isEmpty(filename) )
      return getDefault();

    File file = new File(filename);
    if ( !file.exists() )
      if ( Str.equals(file.getName(), file.getPath()) )
      {
        File alt = new File(Args.defUserDataFolder(), file.getName());
        if ( alt.exists() )
          file = alt;
      }

    return get(file);
  }

  /**
   * Loads the default template. This will either be the internally stored
   * template, or the template for the session file (if it exists).
   *
   * @return The default template.
   * @throws IOException
   */
  public Template getDefault() throws IOException
  {
    Template template = templates.get(DEFAULT_TEMPLATE_KEY);
    if ( template == null )
      synchronized ( lock )
      {
        template = templates.get(DEFAULT_TEMPLATE_KEY);
        if ( template == null )
        {
          template = new Template(defaultTemplateSupplier);
          templates.put(DEFAULT_TEMPLATE_KEY, template);
          LoggerFactory.getLogger(getClass()).trace("Loaded default template.");
        }
      }
    return template;
  }
}
