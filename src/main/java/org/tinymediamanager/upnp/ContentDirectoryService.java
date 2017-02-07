package org.tinymediamanager.upnp;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.UTF8Control;

public class ContentDirectoryService extends AbstractContentDirectoryService {

  private static final Logger         LOGGER = LoggerFactory.getLogger(ContentDirectoryService.class);
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  @Override
  public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults, SortCriterion[] orderby)
      throws ContentDirectoryException {
    try {
      LOGGER.debug("ObjectId:" + objectID);
      LOGGER.debug("BrowseFlag:" + browseFlag);
      LOGGER.debug("Filter:" + filter);
      LOGGER.debug("FirstResult:" + firstResult);
      LOGGER.debug("MaxResults:" + maxResults);
      LOGGER.debug("OrderBy:" + Arrays.toString(orderby));

      DIDLContent didl = new DIDLContent();
      DIDLParser dip = new DIDLParser();

      String[] path = StringUtils.split(objectID, '/');
      // [0] = movie|tvshow
      // [1] = UUID of MediaEntity, if TvShow list

      // get full metadata of one object
      if (browseFlag.equals(BrowseFlag.METADATA)) {
        if (path[0].equals(Upnp.ID_ROOT)) {
          LOGGER.warn("Unable to get Metadata from root object!");
          return new BrowseResult(null, 0, 0);
        }
        else if (path[0].equals(Upnp.ID_MOVIES)) {
          org.tinymediamanager.core.movie.entities.Movie m = MovieList.getInstance().lookupMovie(UUID.fromString(path[1]));
          if (m != null) {
            didl.addItem(Metadata.getUpnpMovie(m, true));
          }
          if (didl.getItems().size() == 1) {
            String ret = dip.generate(didl);
            LOGGER.debug(prettyFormat(ret, 2));
            return new BrowseResult(ret, 1, 1);
          }
          else {
            // check for TV
            LOGGER.warn("no movie with ID " + objectID + " found");
            return new BrowseResult(null, 0, 0);
          }
        }
        else if (path[0].equals(Upnp.ID_TVSHOWS)) {
        }

      }
      else if (browseFlag.equals(BrowseFlag.DIRECT_CHILDREN)) {
        // get "just enough" metadata for directory listing
        // create folder structure and/or items

        if (path[0].equals(Upnp.ID_ROOT)) {
          StorageFolder cont = new StorageFolder();
          cont.setId(Upnp.ID_MOVIES);
          cont.setParentID(Upnp.ID_ROOT);
          cont.setTitle(BUNDLE.getString("tmm.movies"));
          didl.addContainer(cont);

          cont = new StorageFolder();
          cont.setId(Upnp.ID_TVSHOWS);
          cont.setParentID(Upnp.ID_ROOT);
          cont.setTitle(BUNDLE.getString("tmm.tvshows"));
          didl.addContainer(cont);

          String ret = dip.generate(didl);
          LOGGER.debug(prettyFormat(ret, 2));
          return new BrowseResult(ret, 2, 2);
        }
        else if (path[0].equals(Upnp.ID_MOVIES)) {
          // no more levels - build items
          for (org.tinymediamanager.core.movie.entities.Movie m : MovieList.getInstance().getMovies()) {
            didl.addItem(Metadata.getUpnpMovie(m, false));
          }
          int count = didl.getItems().size();
          String ret = dip.generate(didl);
          LOGGER.debug(prettyFormat(ret, 2));
          return new BrowseResult(ret, count, count);
        }
        else if (path[0].equals(Upnp.ID_TVSHOWS)) {

          if (path.length == 1) {
            // build folders for each tvshow
            StorageFolder cont = new StorageFolder();
            for (org.tinymediamanager.core.tvshow.entities.TvShow t : TvShowList.getInstance().getTvShows()) {
              cont = new StorageFolder();
              cont.setId(Upnp.ID_TVSHOWS);
              cont.setParentID(Upnp.ID_ROOT);
              cont.setTitle(t.getTitle());
              didl.addContainer(cont);
            }
            int count = didl.getItems().size();
            String ret = dip.generate(didl);
            LOGGER.debug(prettyFormat(ret, 2));
            return new BrowseResult(ret, count, count);
          }
          else if (path.length == 2) {
            // build items of episodes
            UUID uuid = UUID.fromString(path[1]);
            org.tinymediamanager.core.tvshow.entities.TvShow show = TvShowList.getInstance().lookupTvShow(uuid);
            for (TvShowEpisode ep : show.getEpisodes()) {
              didl.addItem(Metadata.getUpnpTvShowEpisode(show, ep, false));
            }

            int count = didl.getItems().size();
            String ret = dip.generate(didl);
            LOGGER.debug(prettyFormat(ret, 2));
            return new BrowseResult(ret, count, count);
          }
        }
        else {
          LOGGER.warn("Whoops. There was an error in our directory structure.");
        }
      }
      return new BrowseResult(null, 0, 0);
    }
    catch (Exception ex) {
      LOGGER.error("Browse failed", ex);
      throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
    }
  }

  @Override
  public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults, SortCriterion[] orderBy)
      throws ContentDirectoryException {
    // You can override this method to implement searching!
    return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
  }

  public static String prettyFormat(String input, int indent) {
    try {
      Source xmlInput = new StreamSource(new StringReader(input));
      StringWriter stringWriter = new StringWriter();
      StreamResult xmlOutput = new StreamResult(stringWriter);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute("indent-number", indent);
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(xmlInput, xmlOutput);
      return xmlOutput.getWriter().toString();
    }
    catch (Exception e) {
      return "! error parsing xml !";
    }
  }
}
