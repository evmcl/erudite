package com.evanmclean.erudite;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evanmclean.erudite.logback.Logback;
import com.evanmclean.evlib.io.Folders;
import com.google.common.collect.ImmutableList;

/**
 * The main workhorse, a thread that takes articles off a queue and runs them
 * through each processor. Obviously you can run several of these at once on the
 * same queue.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public class ProcessorThread extends Thread
{
  private static final AtomicInteger workerNum = new AtomicInteger();
  private static final Logger log = LoggerFactory
      .getLogger(ProcessorThread.class);

  private final ConcurrentLinkedQueue<Article> workQueue;
  private final Erudite erudite;
  private final Source source;
  private final ImageHandlerFactory ihf;
  private final ImmutableList<Processor> processors;
  private final File workFolder;
  private AtomicBoolean anyErrors = new AtomicBoolean(false);

  /**
   * Create (but don't start) a processor thread.
   * 
   * @param work_queue
   *        The queue of articles to be processed.
   * @param erudite
   *        An {@link Erudite} object to be used for processing.
   * @param source
   *        The source of all the articles.
   * @param ihf
   *        An image factory handler.
   * @param processors
   *        The list of {@link Processor}s to run each article through.
   * @param work_folder
   *        A temporary folder this thread can use to do all its processing.
   */
  public ProcessorThread( final ConcurrentLinkedQueue<Article> work_queue,
      final Erudite erudite, final Source source,
      final ImageHandlerFactory ihf, final ImmutableList<Processor> processors,
      final File work_folder )
  {
    super("eruditeworker" + workerNum.incrementAndGet());
    this.workQueue = work_queue;
    this.erudite = erudite;
    this.source = source;
    this.ihf = ihf;
    this.processors = processors;
    this.workFolder = work_folder;
  }

  /**
   * True if the thread encountered any errors while processing the articles.
   * 
   * @return True if the thread encountered any errors while processing the
   *         articles.
   */
  public boolean anyErrors()
  {
    return anyErrors.get();
  }

  /**
   * Runs in a loop, taking articles off the queue until the queue is empty,
   * then ends.
   */
  @Override
  public void run()
  {
    log.trace("Processor thread started.");
    try
    {
      Article article;
      while ( (article = workQueue.poll()) != null )
        if ( !process(article) )
          anyErrors.set(true);
    }
    finally
    {
      Logback.flushLog();
    }
    log.trace("Processor thread finished.");
  }

  private boolean process( final Article article )
  {
    try
    {
      boolean no_errors = true;
      log.info(article.getTitle());
      try
      {
        if ( (processors == null) || processors.isEmpty() )
        {
          log.error("No processors available for articles.");
          no_errors = false;
        }
        else
        {
          for ( final Processor processor : processors )
            try
            {
              Folders.mksClear(workFolder);
              processor.process(article, erudite, source, ihf, workFolder);
            }
            catch ( Exception ex )
            {
              log.error("Error while processing " + article.getTitle(), ex);
              no_errors = false;
            }
        }
      }
      catch ( Exception ex )
      {
        log.error("Error while processing " + article.getTitle(), ex);
        no_errors = false;
      }

      if ( no_errors )
        try
        {
          source.onComplete(article);
        }
        catch ( IOException ex )
        {
          log.error("Error while processing " + article.getTitle(), ex);
          no_errors = false;
        }

      if ( !no_errors )
        try
        {
          source.onError(article);
        }
        catch ( IOException ex )
        {
          log.error("Error while processing " + article.getTitle(), ex);
        }

      return no_errors;
    }
    finally
    {
      Logback.flushLog();
    }
  }
}
