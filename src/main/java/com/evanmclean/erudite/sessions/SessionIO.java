package com.evanmclean.erudite.sessions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Read and write {@link Session} objects.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public final class SessionIO
{
  public static Session read( final File file )
    throws IOException,
      ClassNotFoundException
  {
    final FileInputStream in = new FileInputStream(file);
    try
    {
      return read(in);
    }
    finally
    {
      in.close();
    }
  }

  public static Session read( final InputStream in )
    throws IOException,
      ClassNotFoundException
  {
    final ObjectInputStream oin = new ObjectInputStream(in);
    return (Session) oin.readObject();
  }

  public static void write( final File file, final Session session )
    throws IOException
  {
    final FileOutputStream out = new FileOutputStream(file);
    try
    {
      write(out, session);
    }
    finally
    {
      out.close();
    }
  }

  public static void write( final OutputStream out, final Session session )
    throws IOException
  {
    final ObjectOutputStream oout = new ObjectOutputStream(out);
    try
    {
      oout.writeObject(session);
    }
    finally
    {
      oout.flush();
    }
  }

  private SessionIO()
  {
    // empty
  }
}
