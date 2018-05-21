<#if for_hugo == "true">
---
title: Erudite
---
</#if>

# Introduction

Erudite will download articles from
[Instapaper](https://www.instapaper.com/) or
[Pocket](https://getpocket.com/), convert them to an appropriate format
such as EPUB or MOBI, and add them to your
[Calibre](http://calibre-ebook.com/) ebook library. It is a command-line
driven tool.

<#if for_website == "true">
This documentation is also available in
[EPUB](${ebook_prefix}erudite.epub) and
[MOBI](${ebook_prefix}erudite.mobi) ebook formats.
</#if>

<#if for_hugo != "true">
[Home Page](http://evanmclean.com/software/erudite/) &bull;
</#if>
[Downloads](https://github.com/evmcl/erudite/releases) &bull;
[GitHub](https://github.com/evmcl/erudite)

Erudite is distributed under the [Apache License
2.0](http://www.apache.org/licenses/LICENSE-2.0).

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>

# How It Works

Erudite takes articles from a source (such as
[Instapaper](https://www.instapaper.com/) or
[Pocket](https://getpocket.com/)) and uses a HTML template file and a set
of configuration properties to produce the appropriately formatted ebook
files and (optionally) add them to your
[Calibre](http://calibre-ebook.com/) ebook library.

![Erudite Components](${image_prefix}erudite_parts.png)<#if for_hugo != "true">\ </#if>

## Session File

The session file holds the details necessary for accessing your
[Instapaper](https://www.instapaper.com/) or
[Pocket](https://getpocket.com/) session.

## Configuration

The configuration file is a standard set of key/value properties that
tell Erudite how to process each article. Erudite first produces a HTML
version of the article in an easy to read format, then uses Calibre's
[`ebook-convert`](http://manual.calibre-ebook.com/cli/ebook-convert.html)
program to convert to the desired file type such as EPUB or MOBI. It can
then either save the file to a folder, or add it to a
[Calibre](http://calibre-ebook.com/) ebook library.

## Template

This is a HTML template used to produce the readable version of an
article. This is optional as Erudite has an default HTML template it
normally will use, but the option is available for advanced
customisation.

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>

# Usage

Run the `erudite.jar` file with no arguments in order to see the full
usage.

        java -jar erudite.jar

The full usage is shown here and may look a little intimidating, but we
will break it down for each function later:

<pre>
<#include "usage_insert.txt" parse=false encoding="UTF-8">
</pre>

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>

## Initialise a Session

Before being able to process articles, you will need to initialise your
session with one of [Instapaper](https://www.instapaper.com/) or
[Pocket](https://getpocket.com/).

You can generate this by running:

        java -jar erudite.jar init instapaper

or

        java -jar erudite.jar init pocket

For an [Instapaper](https://www.instapaper.com/) session, you will be
prompted for your email and password. Neither of these are stored in the
session file, only the session cookies (just like a browser).

[Pocket](https://getpocket.com/) is a little more involved. Erudite will
need your user ID and password, and will also directed to the web site to
generate an API key and grant permission to Erudite to access your
account. Neither the user ID or password are stored in the session file,
only the session cookies (just like a browser).

## Create your Configuration

Before being able to process articles, you will need to create a
configuration file for your session.

You can generate a fully annotated configuration file by running:

        java -jar erudite.jar config

This saves the file in the default location (run `java -jar erudite.jar`
and look at the last few lines to see where this is). Open this with your
preferred text editor, read through the documentation provided within and
edit to suit your needs.

## Process Articles

To download articles and convert them, run:

        java -jar erudite.jar process

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>

## Other Things You Can Do

From the command line, you can also:

### List Articles To Be Processed

To list the articles waiting at the web service to be processed, run:

        java -jar erudite.jar list

### Create a HTML Template

If you need to use a custom HTML template, start with the one used by
Erudite by running:

        java -jar erudite.jar template

or

        java -jar erudite.jar template mytemplate.html

The first one will save the file in the default location (run `java -jar
erudite.jar` and look at the last few lines to see where this is). The
second one saves the file to the specified location. Edit the template to
suit your needs and refer to it from the configuration file if necessary.

### Test Your Title Munging Fu

In the annotated configuration file you are introduced to title munging.
Sometimes the titles for articles need a little bit of cleaning up before
being used in the produced document. Title mungers use regular
expressions to perform substitutions to make titles more readable or
sortable.

e.g., Change "Coding Horror: New Programming Jargon" to "New Programming
Jargon « Coding Horror" so it sorts by the name of the article, not the
name of the site it was from.

Refer to the annotated configuration file to see how title munging works.

After editing the title munging in your configuration file, you can test
it works as expected by running:

        java -jar erudite titletest This is my test title

Wrap the title in quotes if necessary for your command line.

## A note of default file locations.

By default, Erudite will store its data files in the folder `~/.erudite`
for Linux, and `%HOMEPATH%\erudite` for Windows. The default session file
is called `session.dat`.

By default the configuration and template files have the same path and
name as the session file, but with a `.properties` and `.html` extension
respectively.

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>

# Annotated Configuration File

Below is a fully annotated sample configuration file.

<pre>
<#include "sample_config.properties" parse=false encoding="UTF-8">
</pre>

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>

<#if for_website == "true">
<p><small><a href="#top">top</a></small></p>
</#if>
