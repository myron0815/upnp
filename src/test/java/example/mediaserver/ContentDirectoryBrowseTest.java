/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package example.mediaserver;

import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.SortCriterion;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.upnp.ContentDirectoryService;

public class ContentDirectoryBrowseTest {

  private static final String KODI_FILTER = "dc:date,dc:description,upnp:longDescription,upnp:genre,res,res@duration,res@size,upnp:albumArtURI,upnp:rating,upnp:lastPlaybackPosition,upnp:lastPlaybackTime,upnp:playbackCount,upnp:originalTrackNumber,upnp:episodeNumber,upnp:programTitle,upnp:seriesTitle,upnp:album,upnp:artist,upnp:author,upnp:director,dc:publisher,searchable,childCount,dc:title,dc:creator,upnp:actor,res@resolution,upnp:episodeCount,upnp:episodeSeason,xbmc:dateadded,xbmc:rating,xbmc:votes,xbmc:artwork,xbmc:uniqueidentifier,xbmc:country,xbmc:userrating";

  @BeforeClass
  public static void init() throws Exception {
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();
  }

  @AfterClass
  public static void shutdown() throws Exception {
    TvShowModuleManager.getInstance().shutDown();
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void browseRoot() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("0", BrowseFlag.DIRECT_CHILDREN, KODI_FILTER, 0, 200, new SortCriterion[] {});
  }

  @Test
  public void browseMovies() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("1", BrowseFlag.DIRECT_CHILDREN, "", 0, 200, new SortCriterion[] {});
  }

  @Test
  public void browseTvShow() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("2", BrowseFlag.DIRECT_CHILDREN, "", 0, 200, new SortCriterion[] {});
  }

  @Test
  public void metadataRoot() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("0", BrowseFlag.METADATA, KODI_FILTER, 0, 200, new SortCriterion[] {});
  }

  @Test
  public void metadataMovies() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("1", BrowseFlag.METADATA, "", 0, 200, new SortCriterion[] {});
  }

  @Test
  public void metadataTvShow() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("2", BrowseFlag.METADATA, "", 0, 200, new SortCriterion[] {});
  }

  @Test
  public void browseMetadata() throws ContentDirectoryException {
    ContentDirectoryService s = new ContentDirectoryService();
    s.browse("68bcb1d0-cc3f-440a-8de2-8eb82fb7cac5", BrowseFlag.METADATA, "", 1, 1, new SortCriterion[] {});

  }

}
