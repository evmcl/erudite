package com.evanmclean.erudite.logback;

import java.io.File;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import ch.qos.logback.core.util.StatusPrinter;

import com.evanmclean.erudite.ProcessorThread;
import com.evanmclean.evlib.lang.Str;

/**
 * <p>
 * Configures the Logback logging system for our application.
 * </p>
 * 
 * <p>
 * We do what is hopefully a somewhat clever trick were we buffer the log
 * messages destine to the console by the {@link ProcessorThread}s and flush
 * them as each article has been processed. Thus you wont get interleaved
 * messages on the console from the processing of different articles.
 * </p>
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class Logback
{
  private static ThreadLocalAppender<Appender<ILoggingEvent>, ILoggingEvent> localapp;
  private static boolean configured;

  /**
   * Configure Logback.
   * 
   * @param log_file
   *        The log file to rollover and then log to.
   * @param clogging
   *        The type of logging to the console or standard output that we want.
   */
  public static void configure( final File log_file,
      final ConsoleLogging clogging )
  {
    final LoggerContext context = (LoggerContext) LoggerFactory
        .getILoggerFactory();
    context.reset();

    configured = false;
    boolean okay = false;
    try
    {
      final Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      root.setLevel(Level.ALL);

      // Setup file appender.
      {
        final RollingFileAppender<ILoggingEvent> logapp = new RollingFileAppender<ILoggingEvent>();
        logapp.setFile(log_file.getPath());
        logapp.setContext(context);
        logapp.setName("file");
        logapp.setAppend(false);
        {
          final FixedWindowRollingPolicy policy = new FixedWindowRollingPolicy();
          policy.setContext(context);
          policy.setFileNamePattern(log_file.getPath() + ".%i");
          policy.setMinIndex(1);
          policy.setMaxIndex(4);
          policy.setParent(logapp);
          policy.start();
          logapp.setRollingPolicy(policy);
        }
        {
          final TriggeringPolicy<ILoggingEvent> trig = new TriggeringPolicyBase<ILoggingEvent>() {
            @Override
            public boolean isTriggeringEvent( final File arg0,
                final ILoggingEvent arg1 )
            {
              // Never trigger. Always done manually.
              return false;
            }
          };
          trig.start();
          logapp.setTriggeringPolicy(trig);
        }
        {
          final PatternLayoutEncoder enc = new PatternLayoutEncoder();
          enc.setContext(context);
          enc.setPattern("%-8date{H:mm:ss} [%thread] %-5level %logger{15} %message%n");
          enc.start();
          logapp.setEncoder(enc);
        }
        logapp.start();
        logapp.rollover();
        root.addAppender(logapp);
      }

      if ( !ConsoleLogging.SILENT.equals(clogging) )
      {
        // Setup the real console appender.
        final Appender<ILoggingEvent> conapp;
        {
          final ConsoleAppender<ILoggingEvent> parentapp = new ConsoleAppender<ILoggingEvent>();
          parentapp.setContext(context);
          parentapp.setName("realconsole");
          final boolean verbose = ConsoleLogging.VERBOSE.equals(clogging);
          {
            final PatternLayoutEncoder enc = new PatternLayoutEncoder();
            enc.setContext(context);
            enc.setPattern(verbose ? "%-8date{H:mm:ss} %message%n"
                : "%message%n");
            enc.start();
            parentapp.setEncoder(enc);
          }
          parentapp.start();

          conapp = new SynchronizedAppender<ILoggingEvent>(parentapp);
          conapp.setContext(context);
          conapp.setName("syncconsole");
          {
            final ThresholdFilter filter = new ThresholdFilter();
            filter.setContext(context);
            filter.setLevel(verbose ? Level.DEBUG.toString() : Level.INFO
                .toString());
            filter.start();
            conapp.addFilter(filter);
          }
          conapp.start();
        }

        // Setup local threaded appender.
        final boolean quiet = ConsoleLogging.QUIET.equals(clogging);
        localapp = new ThreadLocalAppender<Appender<ILoggingEvent>, ILoggingEvent>(
            new ThreadLocal<Appender<ILoggingEvent>>() {
              @Override
              protected Appender<ILoggingEvent> initialValue()
              {
                final String name = Thread.currentThread().getName();
                if ( Str.startsWithIgnoreCase(name, "eruditeworker") )
                {
                  final Appender<ILoggingEvent> app = new BufferredAppender<ILoggingEvent>(
                      conapp, quiet);
                  app.setContext(context);
                  app.setName(name);
                  app.start();
                  return app;
                }
                return conapp;
              }
            });

        localapp.setContext(context);
        localapp.setName("console");
        localapp.start();
        root.addAppender(localapp);
      }

      okay = true;
      configured = true;
    }
    finally
    {
      if ( !okay )
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }
  }

  /**
   * Configure logback so there is no logging, anywhere.
   */
  public static void devnull()
  {
    final LoggerContext context = (LoggerContext) LoggerFactory
        .getILoggerFactory();
    context.reset();
  }

  /**
   * Flush any buffered log messages for the current thread to the console.
   */
  public static void flushLog()
  {
    if ( localapp != null )
    {
      final Appender<ILoggingEvent> app = localapp.get();
      if ( app instanceof BufferredAppender<?> )
        ((BufferredAppender<?>) app).flush();
    }
  }

  /**
   * True if Logback has been successfully configured.
   * 
   * @return True if Logback has been successfully configured.
   */
  public static boolean isConfigured()
  {
    return configured;
  }
}
