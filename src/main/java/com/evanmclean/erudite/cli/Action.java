package com.evanmclean.erudite.cli;

/**
 * The high-level action to be performed by the application.
 * 
 * @author Evan M<sup>c</sup>Lean, <a href="http://evanmclean.com/"
 *         target="_blank">M<sup>c</sup>Lean Computer Services</a>
 */
public enum Action
{
  /**
   * Initialise a new session.
   */
  INIT(true),
  /**
   * Produce a sample configuration file.
   */
  CONFIG(false),
  /**
   * Produce a sample template file.
   */
  TEMPLATE(false),
  /**
   * Process any available articles from the source.
   */
  PROCESS(true),
  /**
   * List any available articles from the source.
   */
  LIST(true),
  /**
   * Test the title munging on an example title.
   */
  TITLETEST(false);

  /**
   * Get action where <code>str</code> matches that start of the action name
   * (case-insensitive).
   * 
   * @param str
   * @return The matching action.
   * @throws IllegalArgumentException
   *         if <code>str</code> is <code>null</code> or does not match an
   *         action.
   */
  public static Action get( final String str )
  {
    if ( str != null )
    {
      Action match = null;
      final int len = str.length();
      for ( final Action action : Action.values() )
      {
        final String saction = action.name();
        final int alen = saction.length();
        if ( len < alen )
        {
          if ( str.equalsIgnoreCase(saction.substring(0, len)) )
          {
            if ( match != null )
              throw new IllegalArgumentException("No such action: " + str);
            match = action;
          }
        }
        else if ( len == alen )
        {
          if ( str.equalsIgnoreCase(saction) )
          {
            if ( match != null )
              throw new IllegalArgumentException("No such action: " + str);
            match = action;
          }
        }
      }
      if ( match != null )
        return match;
    }
    throw new IllegalArgumentException("No such action: " + str);
  }

  private final boolean needsLogging;

  private Action( final boolean needsLogging )
  {
    this.needsLogging = needsLogging;
  }

  /**
   * Do we need to use logging for this action?
   * 
   * @return Do we need to use logging for this action?
   */
  public boolean isNeedsLogging()
  {
    return needsLogging;
  }
}
