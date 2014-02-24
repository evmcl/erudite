package com.evanmclean.erudite.pocket.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class ActionResults
{
  private final List<Boolean> results;

  public ActionResults(
      @JsonProperty( "action_results" ) final List<Boolean> results )
  {
    this.results = ImmutableList.copyOf(results);
  }

  public ImmutableList<Boolean> getResults()
  {
    return (ImmutableList<Boolean>) results;
  }

  public boolean isSuccessful()
  {
    for ( final Boolean result : results )
      if ( (result == null) || (!result.booleanValue()) )
        return false;
    return true;
  }

  @Override
  public String toString()
  {
    return "ActionResults [results=" + results + "]";
  }
}
