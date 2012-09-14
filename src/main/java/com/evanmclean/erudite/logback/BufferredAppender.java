package com.evanmclean.erudite.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;

/**
 * Stores events in a buffer and only flushes them to the parent appender on a
 * call to <code>flush()</code>.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 * @param <E>
 */
class BufferredAppender<E extends ILoggingEvent> extends ListAppender<E>
{
  private final Appender<E> parent;
  private final boolean quiet;

  /**
   * @param parent
   *        The parent appender to flush to.
   * @param quiet
   *        Only flush the buffered logs if any of them where of an error level.
   */
  BufferredAppender( final Appender<E> parent, final boolean quiet )
  {
    this.parent = parent;
    this.quiet = quiet;
  }

  /**
   * Flush and clear the list of buffered events. The set of events are written
   * to the parent while being <code>synchronized</code> on the parent object.
   */
  void flush()
  {
    if ( doFlush() )
      synchronized ( parent )
      {
        for ( final E event_object : list )
          parent.doAppend(event_object);
      }
    list.clear();
  }

  private boolean doFlush()
  {
    if ( list.isEmpty() )
      return false;
    if ( !quiet )
      return true;
    for ( final E event_object : list )
      if ( Level.ERROR.equals(event_object.getLevel()) )
        return true;
    return false;
  }
}
