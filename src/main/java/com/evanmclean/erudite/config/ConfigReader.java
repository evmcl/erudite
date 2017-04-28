package com.evanmclean.erudite.config;

import java.io.File;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

import com.evanmclean.erudite.cli.Args;
import com.evanmclean.evlib.lang.Arr;
import com.evanmclean.evlib.lang.ArrayIterable;
import com.evanmclean.evlib.lang.Str;
import com.google.common.collect.ImmutableList;

/**
 * Reads the configuration from a file.
 *
 * @author Evan M<sup>c</sup>Lean,
 *         <a href="http://evanmclean.com/" target="_blank">M<sup>c</sup>Lean
 *         Computer Services</a>
 */
public final class ConfigReader
{
  /**
   * Read the configuration from a file. Uses a {@link PropertiesConfiguration}
   * under the hood.
   *
   * @param file
   * @return A {@link Config}.
   * @throws ConfigurationException
   */
  public static Config read( final File file ) throws ConfigurationException
  {
    final PropertiesConfiguration props = new FileBasedConfigurationBuilder<PropertiesConfiguration>(
        PropertiesConfiguration.class)
            .configure(new Parameters().properties() //
                .setIncludesAllowed(true) //
                .setListDelimiterHandler(DisabledListDelimiterHandler.INSTANCE) //
                .setBasePath(Args.defUserDataFolder().toString()) //
                .setThrowExceptionOnMissing(false) //
                .setEncoding("UTF-8") //
                .setFile(file) //
            ).getConfiguration();

    // Get the titles for title munging.
    final TitleMunger titleMunger;
    {
      final TitleMunger.Builder bldr = TitleMunger.builder();
      final String[] arr = props.getStringArray("title");
      if ( arr != null )
        for ( String str : ArrayIterable.create(arr) )
          bldr.add(str);
      titleMunger = bldr.build();
    }

    return new Config() {
      @Override
      public boolean getBoolean( final String key, final boolean def )
      {
        final String str = getString(key, null);
        try
        {
          return ConfigUtils.toBoolean(str, def);
        }
        catch ( IllegalStateException ex )
        {
          throw new IllegalStateException(
              "Invalid boolean value for configuration variable " + key + ": "
                  + str);
        }
      }

      @Override
      public int getInt( final String key, final int def )
      {
        final String str = getString(key, null);
        if ( Str.isEmpty(str) )
          return def;

        try
        {
          return Integer.parseInt(str);
        }
        catch ( NumberFormatException ex )
        {
          throw new IllegalStateException(
              "Invalid integer value for configuration variable " + key + ": "
                  + str);
        }
      }

      @Override
      public String getString( final String key )
      {
        return getString(key, null);
      }

      @Override
      public String getString( final String key, final String def )
      {
        final String[] strings = props.getStringArray(key);
        for ( int xi = Arr.length(strings) - 1; xi >= 0; --xi )
        {
          final String str = Str.trimToNull(strings[xi]);
          if ( str != null )
            return str;
        }
        return def;
      }

      @Override
      public ImmutableList<String> getStrings( final String key )
      {
        final String[] strings = props.getStringArray(key);
        if ( Arr.isEmpty(strings) )
          return ImmutableList.of();

        final ImmutableList.Builder<String> bldr = ImmutableList.builder();
        for ( String str : strings )
        {
          str = Str.trimToNull(str);
          if ( str != null )
            bldr.add(str);
        }
        return bldr.build();
      }

      @Override
      public TitleMunger getTitleMunger()
      {
        return titleMunger;
      }

    };
  }

  private ConfigReader()
  {
    // empty
  }
}
