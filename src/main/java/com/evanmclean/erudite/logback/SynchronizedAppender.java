package com.evanmclean.erudite.logback;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;

/**
 * An appender that only appends the events to the parent while this object is
 * wrapped in a synchronized block.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 * @param <E>
 */
class SynchronizedAppender<E> extends AppenderBase<E>
{
  private final Appender<E> parent;

  SynchronizedAppender( final Appender<E> parent )
  {
    this.parent = parent;
  }

  @Override
  public void setContext( final Context context )
  {
    super.setContext(context);
    parent.setContext(context);
  }

  @Override
  protected void append( final E event_object )
  {
    synchronized ( this )
    {
      parent.doAppend(event_object);
    }
  }
}
