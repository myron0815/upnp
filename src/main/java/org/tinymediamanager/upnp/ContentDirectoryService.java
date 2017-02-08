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

      String[] path = StringUtils.split(objectID, '/');
      // [0] = 0/1/2
      // [1] = UUID
      // [2] = Season
      // [3] = Episode

      // =====================================================
      // get full metadata of object (after clicking on item)
      // =====================================================
      if (browseFlag.equals(BrowseFlag.METADATA)) {

        if (path[0].equals(Upnp.ID_ROOT)) {
          LOGGER.warn("Unable to get Metadata from root object!");
          throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "cannot get metadata from root");
        }
        else if (path[0].equals(Upnp.ID_MOVIES)) {
          if (path.length == 2) {
            org.tinymediamanager.core.movie.entities.Movie m = MovieList.getInstance().lookupMovie(UUID.fromString(path[1]));
            if (m != null) {
              didl.addItem(Metadata.getUpnpMovie(m, true));
              return returnResult(didl);
            }
          }
          throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT, "cannot get metadata for " + objectID);
        }
        else if (path[0].equals(Upnp.ID_TVSHOWS)) {
          if (path.length > 1) {
            org.tinymediamanager.core.tvshow.entities.TvShow t = TvShowList.getInstance().lookupTvShow(UUID.fromString(path[1]));
            if (t != null) {
              TvShowEpisode ep = t.getEpisode(getInt(path[2]), getInt(path[2]));
              if (ep != null) {
                didl.addItem(Metadata.getUpnpTvShowEpisode(t, ep, true));
                return returnResult(didl);
              }
            }
          }
          throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT, "cannot get metadata for " + objectID);
        }

      }
      // =====================================================
      // Browse directory (after clicking on container)
      // =====================================================
      else if (browseFlag.equals(BrowseFlag.DIRECT_CHILDREN)) {

        // create ROOT folder structure (no items)
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

          return returnResult(didl);
        }
        else if (path[0].equals(Upnp.ID_MOVIES)) {
          // create MOVIE folder structure -> items
          for (org.tinymediamanager.core.movie.entities.Movie m : MovieList.getInstance().getMovies()) {
            didl.addItem(Metadata.getUpnpMovie(m, false));
          }
          return returnResult(didl);
        }
        else if (path[0].equals(Upnp.ID_TVSHOWS)) {
          if (path.length == 1) {
            // create TVSHOW folder structure -> container
            StorageFolder cont;
            for (org.tinymediamanager.core.tvshow.entities.TvShow t : TvShowList.getInstance().getTvShows()) {
              cont = new StorageFolder();
              cont.setId(Upnp.ID_TVSHOWS + "/" + t.getDbId());
              cont.setParentID(Upnp.ID_ROOT);
              cont.setTitle(t.getTitle());
              didl.addContainer(cont);
            }
            return returnResult(didl);
          }
          else if (path.length == 2) {
            // create EPISODE items
            UUID uuid = UUID.fromString(path[1]);
            org.tinymediamanager.core.tvshow.entities.TvShow show = TvShowList.getInstance().lookupTvShow(uuid);
            for (TvShowEpisode ep : show.getEpisodes()) {
              didl.addItem(Metadata.getUpnpTvShowEpisode(show, ep, false));
            }
            return returnResult(didl);
          }
        }
        else {
          LOGGER.warn("Whoops. There was an error in our directory structure. " + objectID);
        }
      }
      throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "BrowseFlag wrong " + browseFlag);
    }
    catch (Exception ex) {
      LOGGER.error("Browse failed", ex);
      throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
    }
  }

  private BrowseResult returnResult(DIDLContent didl) throws Exception {
    DIDLParser dip = new DIDLParser();
    int count = didl.getItems().size() + didl.getContainers().size();
    String ret = dip.generate(didl);
    LOGGER.debug(prettyFormat(ret, 2));
    return new BrowseResult(ret, count, count);
  }

  private int getInt(String s) {
    int i = 0;
    try {
      i = Integer.valueOf(s);
    }
    catch (NumberFormatException nfe) {
      LOGGER.warn("Cannot parse number from " + s);
    }
    return i;
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
