package com.evanmclean.erudite.cli;

import java.awt.Desktop;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.Article;
import com.evanmclean.erudite.Articles;
import com.evanmclean.erudite.Erudite;
import com.evanmclean.erudite.ImageHandlerFactory;
import com.evanmclean.erudite.Processor;
import com.evanmclean.erudite.ProcessorThread;
import com.evanmclean.erudite.Source;
import com.evanmclean.erudite.TemplateFactory;
import com.evanmclean.erudite.config.Config;
import com.evanmclean.erudite.config.ConfigReader;
import com.evanmclean.erudite.config.ProcessorsFactory;
import com.evanmclean.erudite.instapaper.Instapaper;
import com.evanmclean.erudite.logback.ConsoleLogging;
import com.evanmclean.erudite.logback.Logback;
import com.evanmclean.erudite.pocket.Pocket;
import com.evanmclean.erudite.readability.Readability;
import com.evanmclean.erudite.sessions.Session;
import com.evanmclean.erudite.sessions.SessionIO;
import com.evanmclean.erudite.sessions.SourceType;
import com.evanmclean.evlib.io.Folders;
import com.evanmclean.evlib.lang.Str;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Main entry point for the command line application.
 *
 * @author Evan M<sup>c</sup>Lean,
 *         <a href="http://evanmclean.com/" target="_blank">M<sup>c</sup>Lean
 *         Computer Services</a>
 */
public class Main
{
  private static final int NO_EXIT = Integer.MIN_VALUE + 5;

  public static void main( final String[] args )
  {
    int ret = 1;
    try
    {
      ret = _main(new Args(args));
    }
    catch ( DisplayHelpException ex )
    {
      if ( Str.isNotEmpty(ex.getMessage()) )
        System.out.println(ex.getMessage());
      Args.printUsage();
    }
    catch ( Exception ex )
    {
      if ( Logback.isConfigured() )
        LoggerFactory.getLogger(Main.class).error("", ex);
      else
        ex.printStackTrace();
    }
    if ( ret != NO_EXIT )
    {
      if ( Logback.isConfigured() )
        LoggerFactory.getLogger(Main.class).trace("Exit value: {}", ret);
      System.exit(ret);
    }
  }

  private static int _main( final Args args ) throws Exception
  {
    if ( args.getAction().isNeedsLogging() )
    {
      Logback.configure(args.getLogFile(), args.getConsoleLogging());
      final Logger log = LoggerFactory.getLogger(Main.class);
      log.debug("Erudite started {}" //
        , new SimpleDateFormat("h:mma EEE d MMM yyyy").format(new Date()));
      log.trace("Console logging level is: " + args.getConsoleLogging());
    }

    switch ( args.getAction() )
    {
      case CONFIG:
        return config(args.getConfigFile());
      case TEMPLATE:
        return template(args.getTemplateFile());
      case INIT:
        return init(args.getSource(), args.getSessionFile());
      case PROCESS:
        return process(args.getSessionFile(), args.getConfigFile(),
          new TemplateFactory(args.getTemplate()),
          ConsoleLogging.QUIET.equals(args.getConsoleLogging()));
      case LIST:
        return list(args.getSessionFile(), args.getConfigFile());
      case TITLETEST:
        return titleTest(args.getTitleToTest(), args.getConfigFile());
    }

    throw new IllegalStateException(
        "Don't know what to do for the action: " + args.getAction());
  }

  private static int config( final File config_file ) throws IOException
  {
    if ( config_file.exists() )
    {
      System.out.println("The configuration file already exists.");
      System.out
          .println("Delete or rename " + config_file.toString() + " first.");
      return 1;
    }

    Args.getSampleConfig().copyTo(Files.asByteSink(config_file));
    System.out
        .println("Generated configuration file: " + config_file.toString());
    return 0;
  }

  private static int init( final SourceType source, final File session_file )
    throws Exception
  {
    final Logger log = LoggerFactory.getLogger(Main.class);

    if ( session_file.exists() )
    {
      log.error("The session file already exists.");
      log.error("Delete or rename {} first.", session_file.toString());
      return 1;
    }

    Folders.mks(session_file.getParentFile());

    final Session session;
    switch ( source )
    {
      case INSTAPAPER:
        session = initInstapaper();
        break;

      case READABILITY:
        session = initReadability();
        break;

      case POCKET:
        session = initPocket();
        break;

      default:
        throw new IllegalStateException(
            "Don't know what to do for the source: " + source);
    }

    if ( session == null )
    {
      log.error("Could not create session.");
      return 1;
    }

    SessionIO.write(session_file, session);

    log.info("Session file written to {}", session_file.toString());

    return 0;
  }

  private static Session initInstapaper() throws IOException
  {
    final Logger log = LoggerFactory.getLogger(Main.class);
    log.info("Need your credentials for Instapaper.");
    log.info(
      "(Note: Your password is not stored. Only session cookies are stored.)");

    final Console console = System.console();
    final String email = Str.trimToNull(console.readLine("Email: "));
    if ( email == null )
    {
      log.error("No email address specified.");
      return null;
    }

    final String pass = String.valueOf(console.readPassword("Pass: "));
    if ( Str.isEmpty(pass) )
    {
      log.error("No password specified.");
      return null;
    }

    return Instapaper.login(email, pass);
  }

  private static Session initPocket() throws Exception
  {
    final Logger log = LoggerFactory.getLogger(Main.class);
    log.info("Need your credentials for Pocket.");
    log.info(
      "(Note: Your password is not stored. Only session cookies are stored.)");

    final Console console = System.console();
    final String user = Str.trimToNull(console.readLine("Username: "));
    if ( user == null )
    {
      log.error("No username address specified.");
      return null;
    }

    final String pass = String.valueOf(console.readPassword("Pass: "));
    if ( Str.isEmpty(pass) )
    {
      log.error("No password specified.");
      return null;
    }

    log.info("Open the URL: {}", Pocket.API_KEY_URL);
    log.info("and create an API key. Enter the consumer key here.");

    final String key = Str.trimToNull(console.readLine("Key: "));
    if ( key == null )
    {
      log.error("No key specified.");
      return null;
    }

    final Pocket.Authoriser authoriser = Pocket.getAuthoriser(user, pass, key);
    final String url = authoriser.getUrl();
    try
    {
      Desktop.getDesktop().browse(new URI(url));
      log.info("Opening {} in browser.", url);
    }
    catch ( Exception ex )
    {
      log.info("Open the URL: {}", url);
    }
    log.info(
      "Click \"Authorize\". Once you are back at the main pocket window, press Enter to continue.");

    console.readLine("Enter to continue: ");

    return authoriser.getSession();
  }

  private static Session initReadability()
  {
    final Logger log = LoggerFactory.getLogger(Main.class);
    log.info("Open the URL: {}", Readability.API_KEY_URL);
    log.info("and create an API key. Enter the key and secret here.");

    final Console console = System.console();
    final String key = Str.trimToNull(console.readLine("Key: "));
    if ( key == null )
    {
      log.error("No key specified.");
      return null;
    }

    final String secret = Str.trimToNull(console.readLine("Secret: "));
    if ( secret == null )
    {
      log.error("No secret specified.");
      return null;
    }

    final Readability.Authoriser authoriser = Readability.getAuthoriser(key,
      secret);
    final String url = authoriser.getUrl();
    try
    {
      Desktop.getDesktop().browse(new URI(url));
      log.info("Opening {} in browser.", url);
    }
    catch ( Exception ex )
    {
      log.info("Open the URL: {}", url);
    }
    log.info("Click \"Allow\" and enter the verifier key here.");

    final String verifier_key = Str
        .trimToNull(console.readLine("Verifier Key: "));
    if ( verifier_key == null )
    {
      log.error("No verifier key specified.");
      return null;
    }

    return authoriser.getSession(verifier_key);
  }

  private static int list( final File session_file, final File config_file )
    throws IOException,
      ClassNotFoundException,
      ConfigurationException
  {
    final Logger log = LoggerFactory.getLogger(Main.class);
    final Session session = SessionIO.read(session_file);
    final Config config = ConfigReader.read(config_file);
    final Source source = session.getSource(config);
    final Articles articles = source.getArticles();
    int ret = 0;

    for ( final String errmsg : articles.getErrors() )
    {
      log.error(errmsg);
      ret = 1;
    }

    if ( articles.getArticles().isEmpty() && articles.getErrors().isEmpty() )
    {
      log.info("No articles to be processed.");
    }
    else
    {
      for ( final Article article : articles.getArticles() )
        log.info(article.getTitle());
    }

    return ret;
  }

  private static int process( final File session_file, final File config_file,
      final TemplateFactory tf, final boolean quiet )
    throws IOException,
      ClassNotFoundException,
      ConfigurationException
  {
    final Logger log = LoggerFactory.getLogger(Main.class);
    final Session session = SessionIO.read(session_file);
    final Config config = ConfigReader.read(config_file);
    final Source source = session.getSource(config);
    final ImmutableList<Processor> processors = ProcessorsFactory.get(config,
      tf);

    int ret = 0;
    final File tmp_folder = Folders.createTempFolder("erudite", ".tmp");
    try
    {
      log.trace("Reading articles from {}", session.getSourceType());
      final Articles arts = source.getArticles();
      for ( final String errmsg : arts.getErrors() )
      {
        log.error(errmsg);
        ret = 1;
      }
      final ConcurrentLinkedQueue<Article> articles = new ConcurrentLinkedQueue<Article>(
          arts.getArticles());
      final int numarticles = articles.size();
      if ( numarticles <= 0 )
      {
        if ( quiet || (!arts.getErrors().isEmpty()) )
          log.debug("No articles to be processed.");
        else
          log.info("No articles to be processed.");
      }
      else
      {
        if ( numarticles == 1 )
          log.trace("There is 1 article to be processed.");
        else
          log.trace("There is {} articles to be processed.", numarticles);

        try
        {
          final Erudite erudite = new Erudite();
          final ProcessorThread[] thrds = new ProcessorThread[Math.min(
            numarticles,
            Math.max(1, Math.min(config.getInt("worker.threads", 1), 20)))];
          final ImageHandlerFactory ihf = ImageHandlerFactory
              .create(thrds.length, config);

          for ( int xi = 0; xi < thrds.length; ++xi )
            thrds[xi] = new ProcessorThread(articles, erudite, source, ihf,
                processors, new File(tmp_folder, "worker" + xi));

          for ( final ProcessorThread thrd : thrds )
            thrd.start();

          log.trace("Waiting on processing threads.");
          for ( final ProcessorThread thrd : thrds )
          {
            thrd.join();
            if ( thrd.anyErrors() )
              ret = 1;
          }
          log.trace("All processing threads complete.");
        }
        catch ( Exception ex )
        {
          log.error("", ex);
          ret = 1;
        }
      }
    }
    finally
    {
      Folders.delQuietly(tmp_folder);
    }

    return ret;
  }

  private static int template( final File template_file ) throws IOException
  {
    if ( template_file.exists() )
    {
      System.out.println("The template file already exists.");
      System.out
          .println("Delete or rename " + template_file.toString() + " first.");
      return 1;
    }

    Args.getInternalTemplate().copyTo(Files.asByteSink(template_file));
    System.out.println("Generated template file: " + template_file.toString());
    return 0;
  }

  private static int titleTest( final String title_to_test,
      final File config_file )
    throws ConfigurationException
  {
    Logback.devnull();
    final String new_title = ConfigReader.read(config_file).getTitleMunger()
        .munge(title_to_test);
    System.out.println(title_to_test);
    if ( Str.equals(title_to_test, new_title) )
      System.out.println("(unchanged)");
    else
      System.out.println(new_title);
    return 0;
  }

  private Main()
  {
    // empty
  }
}
