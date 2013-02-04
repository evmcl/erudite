package com.evanmclean.erudite.readability;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class ReadabilityApi extends DefaultApi10a
{

  @Override
  public String getAccessTokenEndpoint()
  {
    return Readability.BASE_URL + "/oauth/access_token/";
  }

  @Override
  public String getAuthorizationUrl( final Token token )
  {
    return Readability.BASE_URL + "/oauth/authorize/?oauth_token="
        + token.getToken();
  }

  @Override
  public String getRequestTokenEndpoint()
  {
    return Readability.BASE_URL + "/oauth/request_token/";
  }
}
