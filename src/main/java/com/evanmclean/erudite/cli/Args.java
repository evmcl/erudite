package com.evanmclean.erudite.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.evanmclean.erudite.Version;
import com.evanmclean.erudite.logback.ConsoleLogging;
import com.evanmclean.erudite.misc.FileName;
import com.evanmclean.erudite.misc.Utils;
import com.evanmclean.erudite.sessions.SourceType;
import com.evanmclean.evlib.exceptions.UnhandledException;
import com.evanmclean.evlib.io.Files;
import com.evanmclean.evlib.lang.Arr;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.lang.Sys;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

/**
 * Parses the command line arguments.
 *
 * @author Evan M<sup>c</sup>Lean,
 *         <a href="http://evanmclean.com/" target="_blank">M<sup>c</sup>Lean
 *         Computer Services</a>
 */
public class Args
{
  /**
   * Returns the user data folder used by the application. Based on the
   * <code>user.home</code> system property it is usually
   * <code>$HOME\erudite</code> for Windows or <code>$HOME/.erudite</code> for
   * others.
   *
   * @return Returns the user data folder used by the application.
   */
  public static File defUserDataFolder()
  {
    try
    {
      final File home = Sys.userHome();
      final String name = Utils.IS_WINDOWS ? "erudite" : ".erudite";
      return new File(home, name);
    }
    catch ( IOException ex )
    {
      throw new UnhandledException(ex);
    }
  }

  /**
   * Get the internal template that resides in the application's jar file.
   *
   * @return Get the internal template that resides in the application's jar
   *         file.
   */
  public static ByteSource getInternalTemplate()
  {
    return Resources.asByteSource(
      Resources.getResource("com/evanmclean/erudite/template.html"));
  }

  /**
   * Get the sample configuration file that resides in the application's jar
   * file.
   *
   * @return Get the internal template that resides in the application's jar
   *         file.
   */
  public static ByteSource getSampleConfig()
  {
    return Resources.asByteSource(
      Resources.getResource("com/evanmclean/erudite/sample_config.properties"));
  }

  /**
   * Print the command line usage for the application.
   */
  public static void printUsage()
  {
    final File def_session_file = defSessionFile();
    final File def_config_file = defConfigFile(def_session_file);
    final File def_template_file = defTemplateFile(def_session_file);

    System.out.print("Erudite ");
    System.out.println(Version.VERSION);

    System.out.println();

    System.out.println(
      "Initialise a session, you must do this before you can process anything.");
    System.out.println();
    System.out.println("usage: init <source> [<file>]");
    System.out.println();
    System.out.println("    <source>: Where you'll be pulling articles from.");
    System.out.println("              One of \"instapaper\" or \"pocket\".");
    System.out.println();
    System.out.println("    <file>: Where to save the session data.");
    System.out.println("            Default is " + def_session_file.toString());

    System.out.println();

    System.out.println("Produce a fully commented configuration file.");
    System.out.println();
    System.out.println("usage: config [<file>]");
    System.out.println();
    System.out.println("    <file>: Where to save the configuration file.");
    System.out.println("            Default is " + def_config_file.toString());

    System.out.println();

    System.out.println("Produce a copy of the internally used template file.");
    System.out.println();
    System.out.println("usage: template [<file>]");
    System.out.println();
    System.out.println("    <file>: Where to save the template file.");
    System.out
        .println("            Default is " + def_template_file.toString());

    System.out.println();

    System.out.println("Process articles from a session.");
    System.out.println();

    new HelpFormatter().printHelp(
      "process [-v | -q | -S] [-c <file>] [-s <file>]",
      assembleProcessOptions());

    System.out.println();

    System.out.println("List articles to be processed from a session.");
    System.out.println();

    new HelpFormatter().printHelp("list [-v | -q | -S] [-c <file>] [-s <file>]",
      assembleListOptions());

    System.out.println();

    System.out.println("Test the title munging in the configuration file");
    System.out.println();

    new HelpFormatter().printHelp("titletest [-c <file>] <title>",
      assembleTitleTestOptions());

    System.out.println();
    System.out.println("Default session file: " + def_session_file.toString());
    System.out
        .println("Default configuration file: " + def_config_file.toString());
    System.out.println(
      "Default template file (optional): " + def_template_file.toString());
  }

  private static Options assembleListOptions()
  {
    final Options opts = new Options();

    opts.addOption("s", true, "Session file to use.");
    opts.addOption("c", true, "Configuration file to use.");
    opts.addOption("v", false, "Verbose output.");
    return opts;
  }

  private static Options assembleProcessOptions()
  {
    final Options opts = new Options();

    opts.addOption("s", true, "Session file to use.");
    opts.addOption("c", true, "Configuration file to use.");
    opts.addOption("v", false, "Verbose output.");
    opts.addOption("q", false, "Quiet output (only on errors).");
    opts.addOption("S", false, "Silent output.");
    return opts;
  }

  private static Options assembleTitleTestOptions()
  {
    final Options opts = new Options();

    opts.addOption("c", true, "Configuration file to use.");
    return opts;
  }

  private static File defConfigFile( final File session_file )
  {
    return relativeFile(session_file, ".properties");
  }

  private static File defSessionFile()
  {
    return new File(defUserDataFolder(), "session.dat");
  }

  private static File defTemplateFile( final File session_file )
  {
    return relativeFile(session_file, ".html");
  }

  private static File getCanonicalFile( final File file )
  {
    try
    {
      return file.getCanonicalFile();
    }
    catch ( IOException ex )
    {
      return file.getAbsoluteFile();
    }
  }

  private static File getCanonicalFile( final String path )
  {
    if ( Str.isEmpty(path) )
      return null;
    return getCanonicalFile(new File(path));
  }

  /**
   * Produce a new file with the same name, but with the extension replaced.
   *
   * @param file
   *        Original file.
   * @param new_extension
   *        Extension new file should have (must include the leading ".")
   * @return The new file.
   */
  private static File relativeFile( final File file,
      final String new_extension )
  {
    return Files.getCanonicalFile(
      new File(FileName.sansExtension(file) + new_extension));
  }

  private static String[] tail( final String[] strs )
  {
    final int len = Arr.length(strs);
    if ( len <= 0 )
      return (strs != null) ? strs : new String[0];
    if ( len == 1 )
      return new String[0];
    return Arrays.copyOfRange(strs, 1, len);
  }

  private final Action action;
  private final ConsoleLogging consoleLogging;
  private final File configFile;
  private final File templateFile;
  private final File sessionFile;
  private final SourceType source;

  private final String titleToTest;

  Args( final String[] cmdline ) throws DisplayHelpException
  {
    try
    {
      if ( Arr.length(cmdline) <= 0 )
        throw new DisplayHelpException();

      action = Action.get(cmdline[0]);

      switch ( action )
      {
        case INIT:
          if ( (cmdline.length < 2) || (cmdline.length > 3) )
            throw new DisplayHelpException();

          source = SourceType.get(cmdline[1]);

          if ( cmdline.length >= 3 )
            sessionFile = getCanonicalFile(cmdline[2]);
          else
            sessionFile = defSessionFile();

          consoleLogging = ConsoleLogging.NORMAL;
          configFile = null;
          templateFile = null;
          titleToTest = null;
          break;

        case CONFIG:
          if ( cmdline.length >= 2 )
          {
            if ( cmdline.length > 2 )
              throw new DisplayHelpException();
            configFile = getCanonicalFile(cmdline[1]);
          }
          else
          {
            configFile = defConfigFile(defSessionFile());
          }

          consoleLogging = ConsoleLogging.NORMAL;
          templateFile = null;
          sessionFile = null;
          source = null;
          titleToTest = null;
          break;

        case TEMPLATE:
          if ( cmdline.length >= 2 )
          {
            if ( cmdline.length > 2 )
              throw new DisplayHelpException();
            templateFile = getCanonicalFile(cmdline[1]);
          }
          else
          {
            templateFile = defTemplateFile(defSessionFile());
          }

          consoleLogging = ConsoleLogging.NORMAL;
          configFile = null;
          sessionFile = null;
          source = null;
          titleToTest = null;
          break;

        case TITLETEST:
        {
          final CommandLine args = new DefaultParser()
              .parse(assembleTitleTestOptions(), tail(cmdline));

          if ( args.hasOption('c') )
            configFile = getCanonicalFile(args.getOptionValue('c'));
          else
            configFile = defConfigFile(defSessionFile());

          consoleLogging = ConsoleLogging.NORMAL;
          templateFile = null;
          sessionFile = null;
          source = null;

          titleToTest = Str.join(' ', args.getArgs());
          if ( Str.isEmpty(titleToTest) )
            throw new DisplayHelpException();
        }
          break;

        case PROCESS:
        {
          final CommandLine args = new DefaultParser()
              .parse(assembleProcessOptions(), tail(cmdline));

          final boolean verbose = args.hasOption('v');
          final boolean quiet = args.hasOption('q');
          final boolean silent = args.hasOption('S');
          if ( verbose )
          {
            consoleLogging = ConsoleLogging.VERBOSE;
            if ( quiet || silent )
              throw new DisplayHelpException();
          }
          else if ( quiet )
          {
            consoleLogging = ConsoleLogging.QUIET;
            if ( verbose || silent )
              throw new DisplayHelpException();
          }
          else if ( silent )
          {
            consoleLogging = ConsoleLogging.SILENT;
            if ( quiet || verbose )
              throw new DisplayHelpException();
          }
          else
          {
            consoleLogging = ConsoleLogging.NORMAL;
          }

          if ( args.hasOption('s') )
            sessionFile = getCanonicalFile(args.getOptionValue('s'));
          else
            sessionFile = defSessionFile();

          if ( args.hasOption('c') )
            configFile = getCanonicalFile(args.getOptionValue('c'));
          else
            configFile = defConfigFile(sessionFile);

          {
            final File file = defTemplateFile(sessionFile);
            templateFile = file.exists() ? file : null;
          }

          source = null;
          titleToTest = null;

          if ( Arr.isNotEmpty(args.getArgs()) )
            throw new DisplayHelpException();
        }
          break;

        case LIST:
        {
          final CommandLine args = new DefaultParser()
              .parse(assembleListOptions(), tail(cmdline));

          consoleLogging = args.hasOption('v') ? ConsoleLogging.VERBOSE
              : ConsoleLogging.NORMAL;

          if ( args.hasOption('s') )
            sessionFile = getCanonicalFile(args.getOptionValue('s'));
          else
            sessionFile = defSessionFile();

          if ( args.hasOption('c') )
            configFile = getCanonicalFile(args.getOptionValue('c'));
          else
            configFile = defConfigFile(sessionFile);

          templateFile = null;
          source = null;
          titleToTest = null;

          if ( Arr.isNotEmpty(args.getArgs()) )
            throw new DisplayHelpException();
        }
          break;

        default:
          throw new DisplayHelpException();
      }
    }
    catch ( ParseException ex )
    {
      throw new DisplayHelpException(ex);
    }
    catch ( IllegalArgumentException ex )
    {
      throw new DisplayHelpException(ex);
    }
  }

  /**
   * The action to be performed the the application.
   *
   * @return The action to be performed the the application.
   */
  public Action getAction()
  {
    return action;
  }

  /**
   * The configuration file that is being used.
   *
   * @return The configuration file that is being used.
   */
  public File getConfigFile()
  {
    return configFile;
  }

  /**
   * The level of logging that goes to the standard output.
   *
   * @return The level of logging that goes to the standard output.
   */
  public ConsoleLogging getConsoleLogging()
  {
    return consoleLogging;
  }

  /**
   * The log file to write to.
   *
   * @return The log file to write to.
   */
  public File getLogFile()
  {
    if ( sessionFile == null )
      throw new IllegalStateException();
    String name = sessionFile.getName();
    final int pos = name.lastIndexOf('.');
    if ( pos > 0 )
      name = name.substring(0, pos);
    name += ".log";
    return getCanonicalFile(new File(sessionFile.getParentFile(), name));
  }

  /**
   * The session file that is being used.
   *
   * @return The session file that is being used.
   */
  public File getSessionFile()
  {
    return sessionFile;
  }

  /**
   * The type of source for articles (e.g., Instapaper) that is being used.
   *
   * @return The type of source for articles (e.g., Instapaper) that is being
   *         used.
   */
  public SourceType getSource()
  {
    return source;
  }

  /**
   * The template to be used (unless overridden in the configuration file).
   *
   * @return The template to be used (unless overridden in the configuration
   *         file).
   */
  public ByteSource getTemplate()
  {
    if ( templateFile != null )
      return com.google.common.io.Files.asByteSource(templateFile);
    return getInternalTemplate();
  }

  /**
   * Path to the template file (if it is not using the one stored internally in
   * the jar file.)
   *
   * @return Path to the template file (if it is not using the one stored
   *         internally in the jar file.)
   */
  public File getTemplateFile()
  {
    return templateFile;
  }

  /**
   * For {@link Action#TITLETEST}, the title to be tested.
   *
   * @return For {@link Action#TITLETEST}, the title to be tested.
   */
  public String getTitleToTest()
  {
    return titleToTest;
  }
}
