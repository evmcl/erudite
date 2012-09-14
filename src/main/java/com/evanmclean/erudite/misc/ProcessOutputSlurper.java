package com.evanmclean.erudite.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import com.evanmclean.evlib.exceptions.UnhandledException;
import com.evanmclean.evlib.lang.Str;
import com.evanmclean.evlib.util.Colls;

/**
 * Runs a background thread to pull the standard input from a {@link Process}
 * and save as a list of lines.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 * @see Process#getInputStream()
 */
public class ProcessOutputSlurper
{
  private class BkgThread extends Thread
  {
    private final InputStream istr;
    private final BufferedReader in;

    BkgThread( final Process proc )
    {
      this.istr = proc.getInputStream();
      this.in = new BufferedReader(new InputStreamReader(istr));
    }

    /**
     * Runs until the input stream on the process closes.
     */
    @SuppressWarnings( "synthetic-access" )
    @Override
    public void run()
    {
      try
      {
        try
        {
          String line;
          while ( (line = in.readLine()) != null )
          {
            line = Str.trimToNull(line);
            if ( line != null )
              lines.add(line);
          }
        }
        catch ( Exception ex )
        {
          thrdex = ex;
        }
        finally
        {
          istr.close();
        }
      }
      catch ( Exception ex )
      {
        thrdex = ex;
      }
    }
  }

  private static final long SUB_PROC_TIMEOUT = 30L * 60L * 60L * 1000L;

  private final List<String> lines = Colls.newArrayList();
  private BkgThread thrd;
  private Exception thrdex = null;

  /**
   * Runs a background thread that reads the input stream on a process until it
   * closes.
   * 
   * @param proc
   *        The process to read from.
   * @see Process#getInputStream()
   */
  public ProcessOutputSlurper( final Process proc )
  {
    BkgThread newthrd = new BkgThread(proc);
    newthrd.start();
    thrd = newthrd;
  }

  /**
   * Returns the lines of output from the process. Waits until the process is
   * finished.
   * 
   * @return A list of the lines of output from the process. Each line is
   *         trimmed of leading and trailing white space, and empty lines are
   *         ignored.
   * @throws IOException
   *         If there was an exception while the background thread was reading
   *         the input.
   */
  public List<String> getLines() throws IOException
  {
    if ( thrd != null )
      try
      {
        thrd.join(SUB_PROC_TIMEOUT);
        thrd = null;
      }
      catch ( InterruptedException ex )
      {
        throw new UnhandledException(ex);
      }

    if ( thrdex != null )
    {
      if ( thrdex instanceof RuntimeException )
        throw (RuntimeException) thrdex;
      if ( thrdex instanceof IOException )
        throw (IOException) thrdex;
      throw new UnhandledException(thrdex);
    }

    return Collections.unmodifiableList(lines);
  }
}
