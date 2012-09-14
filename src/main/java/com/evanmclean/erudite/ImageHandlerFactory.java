package com.evanmclean.erudite;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.misc.Conn;
import com.evanmclean.erudite.misc.FileName;
import com.evanmclean.erudite.misc.UniqueFile;
import com.evanmclean.evlib.io.Folders;
import com.evanmclean.evlib.lang.Arr;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.stringtransform.FilenameTransformer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;

/**
 * Produces {@link ImageHandler}s. The factory and the handlers it produces are
 * thread safe.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class ImageHandlerFactory
{
  private static class HadIOException extends IOException
  {
    public HadIOException( final String msg )
    {
      super(msg);
    }
  }

  /**
   * <p>
   * The contents of an image, plus the file name extension we should use.
   * </p>
   * <p>
   * If content is null, then:
   * </p>
   * <ul>
   * <li>if extension is also null then the image is unavailable and should be
   * removed from the article.</li>
   * <li>otherwise an exception occurred while retrieving the image, and
   * extension contains the message to go in the IOException.</li>
   * </ul>
   */
  private static class ImageContent
  {
    private final byte[] content;
    private final String name;
    private final String extension;

    ImageContent( final byte[] content, final String name,
        final String extension )
    {
      this.content = content;
      this.name = name;
      this.extension = extension;
    }

    public String getExtension()
    {
      return extension;
    }

    public String getName()
    {
      return name;
    }

    /**
     * Returns true if there is image content available to be saved.
     * 
     * @return True if there is image content available to be saved.
     */
    boolean isAvailable()
    {
      return content != null;
    }

    /**
     * Save a byte array to a file.
     * 
     * @param file
     * @param content
     * @throws IOException
     */
    void save( final File file ) throws IOException
    {
      if ( content == null )
        throw new IllegalStateException("There is no content for this image.");
      final OutputStream out = new FileOutputStream(file);
      try
      {
        out.write(content);
      }
      finally
      {
        out.close();
      }
    }

    /**
     * Throws an IOException if an exception occurred while retrieving the
     * image.
     * 
     * @throws HadIOException
     */
    void throwIfInError() throws HadIOException
    {
      if ( (content == null) && (extension != null) )
        throw new HadIOException(extension);
    }

    /**
     * Used by cache management.
     * 
     * @return Rough number of bytes this object uses.
     */
    int weight()
    {
      return Math.max(1, //
        ((content == null) ? 0 : content.length) //
            + (Str.length(name) * 2) //
            + (Str.length(extension) * 2));
    }
  }

  /**
   * Create factory with unlimited cache (and default concurrency).
   * 
   * @param null_on_error
   *        If there was an error loading an image, just return
   *        <code>null</code> instead of throwing an {@link IOException}.
   * @param convert_to_png
   *        Convert all downloaded images to PNG. This can reduce the
   *        possibility of weird images (such as animated gifs) from making a
   *        document unloadable by your ereader device.
   * @param min_width
   *        Minimum width of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @param min_height
   *        Minimum height of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @return Image handler factory.
   */
  public static ImageHandlerFactory create( final boolean null_on_error,
      final boolean convert_to_png, final int min_width, final int min_height )
  {
    return new ImageHandlerFactory(null_on_error, convert_to_png, min_width,
        min_height, 0, -1L, -1L);
  }

  /**
   * Create factory with unlimited cache.
   * 
   * @param null_on_error
   *        If there was an error loading an image, just return
   *        <code>null</code> instead of throwing an {@link IOException}.
   * @param convert_to_png
   *        Convert all downloaded images to PNG. This can reduce the
   *        possibility of weird images (such as animated gifs) from making a
   *        document unloadable by your ereader device.
   * @param min_width
   *        Minimum width of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @param min_height
   *        Minimum height of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @param cache_concurrency
   *        Basically the expected number of threads that are expected to be
   *        accessing the image cache at once.
   * @return Image handler factory.
   */
  public static ImageHandlerFactory create( final boolean null_on_error,
      final boolean convert_to_png, final int min_width, final int min_height,
      final int cache_concurrency )
  {
    return new ImageHandlerFactory(null_on_error, convert_to_png, min_width,
        min_height, cache_concurrency, -1L, -1L);
  }

  /**
   * Create factory.
   * 
   * @param null_on_error
   *        If there was an error loading an image, just return
   *        <code>null</code> instead of throwing an {@link IOException}.
   * @param convert_to_png
   *        Convert all downloaded images to PNG. This can reduce the
   *        possibility of weird images (such as animated gifs) from making a
   *        document unloadable by your ereader device.
   * @param min_width
   *        Minimum width of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @param min_height
   *        Minimum height of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @param cache_concurrency
   *        Basically the expected number of threads that are expected to be
   *        accessing the image cache at once.
   * @param max_cache_members
   *        The maximum number of images to keep in the cache at once (
   *        <code>-1</code> for unlimited, do not specify a non-negative one
   *        value for both <code>max_cache_members</code> and
   *        <code>max_cache_mb</code> at the same time.)
   * @param max_cache_mb
   *        The maximum number of megabytes the cache should store (
   *        <code>-1L</code> for unlimited, do not specify a non-negative one
   *        value for both <code>max_cache_members</code> and
   *        <code>max_cache_mb</code> at the same time.)
   * @return Image handler factory.
   */
  public static ImageHandlerFactory create( final boolean null_on_error,
      final boolean convert_to_png, final int min_width, final int min_height,
      final int cache_concurrency, final long max_cache_members,
      final long max_cache_mb )
  {
    return new ImageHandlerFactory(null_on_error, convert_to_png, min_width,
        min_height, cache_concurrency, max_cache_members, max_cache_mb);
  }

  /**
   * Create an iamge factory based on settings in the configuation file (see
   * user documentation.)
   * 
   * @param config
   *        Configuration to use.
   * @return Image handler factory.
   */
  public static ImageHandlerFactory create( final Config config )
  {
    return create( //
      config.getInt("worker.threads", 1) //
      , config);
  }

  /**
   * Create an iamge factory based on settings in the configuation file (see
   * user documentation.)
   * 
   * @param cache_concurrency
   *        Basically the expected number of threads that are expected to be
   *        accessing the image cache at once.
   * @param config
   *        Configuration to use.
   * @return Image handler factory.
   */
  public static ImageHandlerFactory create( final int cache_concurrency,
      final Config config )
  {
    final int maxmembers = config.getInt("image.cache.max.members", -1);
    int maxmb = config.getInt("image.cache.max.mb", -1);
    if ( maxmembers >= 0 )
      maxmb = 0;
    else if ( maxmb < 0 )
      maxmb = 100;

    return new ImageHandlerFactory( //
        config.getBoolean("image.ignore.errors", false) //
        , config.getBoolean("image.to.png", false) //
        , config.getInt("image.min.width", 0) //
        , config.getInt("image.min.height", 0) //
        , Math.max(1, Math.min(20, cache_concurrency)) //
        , maxmembers //
        , maxmb //
    );
  }

  /**
   * Create factory that does not cache.
   * 
   * @param null_on_error
   *        If there was an error loading an image, just return
   *        <code>null</code> instead of throwing an {@link IOException}.
   * @param convert_to_png
   *        Convert all downloaded images to PNG. This can reduce the
   *        possibility of weird images (such as animated gifs) from making a
   *        document unloadable by your ereader device.
   * @param min_width
   *        Minimum width of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @param min_height
   *        Minimum height of an image in order to be included in the output.
   *        This is helpful to eliminate less useful things like web bugs.
   * @return Image handler factory.
   */
  public static ImageHandlerFactory createNoCache( final boolean null_on_error,
      final boolean convert_to_png, final int min_width, final int min_height )
  {
    return new ImageHandlerFactory(null_on_error, convert_to_png, min_width,
        min_height, 0, 0, -1L);
  }

  private final boolean nullOnError;
  private final boolean convertToPng;
  private final int minWidth;
  private final int minHeight;
  private final LoadingCache<String, ImageContent> cache;
  private final FilenameTransformer filenameTransformer = new FilenameTransformer(
      FilenameTransformer.NonAsciiHandling.ASCIIFY, 200);
  private final Logger log = LoggerFactory.getLogger(ImageHandlerFactory.class);
  private final Pattern RECURSIVE_URL = Pattern
      .compile("^https?:\\/\\/.+%5[cC]%22(https?:\\/\\/.+)%5[cC]%22$");

  private ImageHandlerFactory( final boolean null_on_error,
      final boolean convert_to_png, final int min_width, final int min_height,
      final int cache_concurrency, final long max_cache_members,
      final long max_cache_mb )
  {
    this.nullOnError = null_on_error;
    this.convertToPng = convert_to_png;
    this.minWidth = Math.max(0, min_width);
    this.minHeight = Math.max(0, min_height);

    final CacheBuilder<String, ImageContent> bldr = CacheBuilder.newBuilder()
        .softValues()
        .removalListener(new RemovalListener<String, ImageContent>() {
          @SuppressWarnings( "synthetic-access" )
          @Override
          public void onRemoval(
              final RemovalNotification<String, ImageContent> event )
          {
            final String cause;
            switch ( event.getCause() )
            {
              case COLLECTED:
                cause = "its memory was released";
                break;
              case EXPIRED:
                cause = "it had expired";
                break;
              case EXPLICIT:
                cause = "it was manually removed";
                break;
              case REPLACED:
                cause = "it was replaced with a new value";
                break;
              case SIZE:
                cause = "space was needed";
                break;
              default:
                cause = event.getCause().toString();
                break;
            }
            log.trace("Removed image from cache because {}: {}", cause,
              event.getKey());
          }
        });

    if ( cache_concurrency >= 0 )
      bldr.concurrencyLevel(Math.min(20, cache_concurrency));
    if ( max_cache_members >= 0 )
      bldr.maximumSize(max_cache_members);
    if ( max_cache_mb >= 0 )
    {
      bldr.maximumWeight(Math.min(1024L, max_cache_mb) * 1024L * 1024L);
      bldr.weigher(new Weigher<String, ImageContent>() {
        @Override
        public int weigh( final String url, final ImageContent image )
        {
          return image.weight() + (url.length() * 2);
        }
      });
    }

    cache = bldr.build(new CacheLoader<String, ImageContent>() {
      @SuppressWarnings( "synthetic-access" )
      @Override
      public ImageContent load( final String url )
      {
        try
        {
          log.trace("Retriving image: {}", url);
          final ImageContent image;
          {
            // Retrieve image content from URL.
            final Connection conn = Conn.connect(url);
            final Response resp = conn.execute();
            if ( resp.statusCode() != 200 )
              throw new IOException("GET " + url + " returned "
                  + resp.statusCode() + ": " + resp.statusMessage());
            final byte[] content = resp.bodyAsBytes();
            if ( Arr.isEmpty(content) )
              throw new IOException("GET " + url + " returned zero bytes.");

            String name = getName(url);
            String ext = getExt(url);
            if ( ext == null )
            {
              ext = getExt(resp);
              if ( ext == null )
              {
                ext = getExt(content);
                if ( ext == null )
                  throw new IOException("Unknown extension for URL " + url);
              }
            }
            image = new ImageContent(content //
                , filenameTransformer.transform(name, "image") //
                , filenameTransformer.transform(ext, ".image").toLowerCase());
          }

          if ( (minWidth > 0) || (minHeight > 0) || convertToPng )
          {
            final BufferedImage bimage;
            final InputStream in = new ByteArrayInputStream(image.content);
            try
            {
              bimage = ImageIO.read(in);
            }
            finally
            {
              in.close();
            }

            if ( (bimage.getWidth() <= minWidth)
                || (bimage.getHeight() <= minHeight) )
            {
              log.trace("Image too small to be used: {}", url);
              return new ImageContent(null, null, null);
            }

            if ( convertToPng )
            {
              final ImageContent png = convert(bimage, image.getName());
              log.trace("Retrieved image and converted to PNG: {}", url);
              return png;
            }
          }

          log.trace("Retrieved image: {}", url);
          return image;
        }
        catch ( IOException ex )
        {
          log.trace("Exception while retriving image: " + url, ex);
          return new ImageContent(null, null, ex.getMessage());
        }
      }

      private ImageContent convert( final BufferedImage image, final String name )
        throws IOException
      {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
          ImageIO.write(image, "png", out);
        }
        finally
        {
          out.close();
        }
        return new ImageContent(out.toByteArray(), name, ".png");
      }

      private String getExt( final byte[] content )
      {
        final MimeUtil2 mu = new MimeUtil2();
        mu.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");

        final Collection<?> mimetypes = mu.getMimeTypes(content);
        if ( (mimetypes == null) || mimetypes.isEmpty() )
          return null;
        final MimeType mt = MimeUtil2.getMostSpecificMimeType(mimetypes);
        if ( (mt == null) || MimeUtil2.UNKNOWN_MIME_TYPE.equals(mt) )
          return null;

        if ( !"image".equals(mt.getMediaType()) )
          return null;

        return "." + mt.getSubType();
      }

      private String getExt( final Response resp )
      {
        final String mimetype = resp.header("Content-Type");
        final int end;
        {
          final int pos = mimetype.indexOf(' ');
          end = (pos > 0) ? pos : mimetype.length();
        }
        if ( (end <= 7) || (!mimetype.startsWith("image/")) )
          return null;

        return "." + mimetype.substring(6, end);
      }

      private String getExt( final String source )
      {
        final int end = getUrlEnd(source);
        final String ext = FileName.extension(source.substring(0, end));
        final int extlen = ext.length();
        if ( (extlen > 0) && (extlen <= 3) )
          return "." + ext;
        return null;
      }

      private String getName( final String source )
      {
        final int end = getUrlEnd(source);
        return Str.ifEmpty(
          FileName.sansExtension(FileName.baseName(source.substring(0, end))),
          "image");
      }

      private int getUrlEnd( final String url )
      {
        if ( url == null )
          return 0;
        int pos = url.indexOf('?');
        if ( pos > 0 )
          return pos;
        return url.length();
      }
    });
  }

  /**
   * Removes all images in the cache.
   */
  public void clearCache()
  {
    cache.invalidateAll();
  }

  /**
   * Get an image handler that uses the cache provided by this factory.
   * 
   * @param folder
   *        Where to save the image file. Will always create a file with a
   *        unique name.
   * @param prefix
   *        A prefix to be added to the name of the file that is returned by
   *        {@link ImageHandler#image(String)}. This will usually represent the
   *        path to the image file relative to the HTML file being produced.
   * @return A new image handler.
   */
  public ImageHandler get( final File folder, final String prefix )
  {
    return new ImageHandler() {
      @SuppressWarnings( "synthetic-access" )
      @Override
      public String image( final String source ) throws IOException
      {
        final String url = sourceToUrl(source);
        if ( url == null )
        {
          log.trace("Not a valid URL for an image: {}", source);
          return null;
        }

        try
        {
          final ImageContent image = cache.get(url);
          if ( !image.isAvailable() )
          {
            image.throwIfInError();
            return null;
          }

          final File file;
          final String path;
          {
            final UniqueFile uf = new UniqueFile(folder, image.getName(),
                image.getExtension());
            file = uf.getFile();
            path = (prefix == null) ? file.getName()
                : (prefix + file.getName());
          }

          if ( !folder.exists() )
            Folders.mks(folder);

          image.save(file);

          log.trace("Saved image {} to {}", url, file);
          return path;
        }
        catch ( HadIOException ex )
        {
          if ( nullOnError )
            return null;
          throw new IOException("Error retrieving " + url, ex);
        }
        catch ( Exception ex )
        {
          throw new IOException("Error processing " + url, ex);
        }
      }

      private String sourceToUrl( final String source )
      {
        if ( (Str.length(source) < 8)
            || ((!source.startsWith("http://")) && (!source
                .startsWith("https://"))) )
        {
          // Not a valid HTTP protocol URL, skip it.
          return null;
        }

        {
          // Occasionally seem to get this funny URL in a URL situation
          // (especially with articles from readwriteweb.com. Think it's
          // something with the way Instapaper saves the text. If this happens,
          // just call ourself with the inner URL.
          @SuppressWarnings( "synthetic-access" ) final Matcher mat = RECURSIVE_URL
              .matcher(source);
          if ( mat.matches() )
            return sourceToUrl(mat.group(1));
        }

        return source;
      }
    };
  }
}
