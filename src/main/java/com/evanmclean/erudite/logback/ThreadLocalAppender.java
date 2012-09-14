package com.evanmclean.erudite.logback;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

/**
 * And appender where events are appended to an internally maintained thread
 * local appender.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 * @param <A>
 * @param <E>
 */
class ThreadLocalAppender<A extends Appender<E>, E> extends AppenderBase<E>
{
  private final ThreadLocal<A> appenders;

  /**
   * @param appenders
   *        The thread local appender allocator.
   */
  ThreadLocalAppender( final ThreadLocal<A> appenders )
  {
    this.appenders = appenders;
  }

  @Override
  protected void append( final E event_object )
  {
    appenders.get().doAppend(event_object);
  }

  A get()
  {
    return appenders.get();
  }

  void set( final A appender )
  {
    appenders.set(appender);
  }
}
