package org.tinymediamanager.upnp;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.DC;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.fourthline.cling.support.model.item.Movie;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.MovieActor;
import org.tinymediamanager.core.movie.entities.MovieProducer;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.scraper.entities.MediaGenres;
import org.tinymediamanager.thirdparty.NetworkUtil;

public class ContentDirectoryService extends AbstractContentDirectoryService {

  private static final String ID_ROOT    = "0";                              // fix, do not change
  private static final String ID_MOVIES  = "1";
  private static final String ID_TVSHOWS = "2";

  private static final String IP         = NetworkUtil.getMachineIPAddress();

  @Override
  public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults, SortCriterion[] orderby)
      throws ContentDirectoryException {
    try {
      // ITEMS:
      // https://github.com/4thline/cling/tree/master/support/src/main/java/org/fourthline/cling/support/model/item

      System.out.println("ObjectId:" + objectID);
      System.out.println("BrowseFlag:" + browseFlag);
      System.out.println("Filter:" + filter);
      System.out.println("FirstResult:" + firstResult);
      System.out.println("MaxResults:" + maxResults);
      System.out.println("OrderBy:" + Arrays.toString(orderby));

      DIDLContent didl = new DIDLContent();
      DIDLParser dip = new DIDLParser();

      if (objectID.equals(ID_ROOT) && browseFlag.equals(BrowseFlag.METADATA)) {
        // no root metadata - should add containers?! TBC
        return new BrowseResult(null, 0, 0);
      }
      else if (objectID.equals(ID_ROOT) && browseFlag.equals(BrowseFlag.DIRECT_CHILDREN)) {
        // build first level structure
        StorageFolder cont = new StorageFolder();
        cont.setId(ID_MOVIES);
        cont.setParentID(ID_ROOT);
        cont.setTitle("Movies");
        didl.addContainer(cont);

        cont = new StorageFolder();
        cont.setId(ID_TVSHOWS);
        cont.setParentID(ID_ROOT);
        cont.setTitle("TV Shows");
        didl.addContainer(cont);

        String ret = dip.generate(didl);
        System.out.println(prettyFormat(ret, 2));
        return new BrowseResult(ret, 2, 2);
      }

      if (browseFlag.equals(BrowseFlag.METADATA)) {
        // get specific objectID and ALL the metadata
        org.tinymediamanager.core.movie.entities.Movie m = MovieList.getInstance().lookupMovie(UUID.fromString(objectID));
        if (m != null) {
          didl.addItem(getUpnpMovie(m, true));
        }

        if (didl.getItems().size() == 1) {
          String ret = dip.generate(didl);
          System.out.println(prettyFormat(ret, 2));
          return new BrowseResult(ret, 1, 1);
        }
        else {
          // check for TV
          System.err.println("no movie with ID found - check TV");
        }
      }

      int count = 0;
      // just add basic things like title - no complete metadata needed (might be too big/slow)
      if (ID_MOVIES.equals(objectID)) {
        for (org.tinymediamanager.core.movie.entities.Movie m : MovieList.getInstance().getMovies()) {
          didl.addItem(getUpnpMovie(m, false));
        }
        count = didl.getItems().size();
      }
      else if (ID_TVSHOWS.equals(objectID)) {
        for (org.tinymediamanager.core.tvshow.entities.TvShow m : TvShowList.getInstance().getTvShows()) {
          // didl.addItem(getUpnpTvShow(m, false));
        }
        count = didl.getItems().size();
      }

      String ret = dip.generate(didl);
      System.out.println(prettyFormat(ret, 2));
      return new BrowseResult(ret, count, count);

    }
    catch (Exception ex) {
      ex.printStackTrace();
      throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
    }
  }

  @Override
  public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults, SortCriterion[] orderBy)
      throws ContentDirectoryException {
    // You can override this method to implement searching!
    return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
  }

  /**
   * wraps a TMM movie into a UPNP movie/video item object
   * 
   * @param tmmMovie
   *          out movie
   * @param full
   *          full details, or when false just the mandatory for a directory listing (title, and a few others)
   * @return
   */
  private Movie getUpnpMovie(org.tinymediamanager.core.movie.entities.Movie tmmMovie, boolean full) {

    System.out.println(tmmMovie.getTitle());
    Movie m = new Movie();
    try {
      m.setId(tmmMovie.getDbId().toString());
      m.setParentID(ID_MOVIES);
      m.addProperty(new DC.DATE(tmmMovie.getYear())); // no setDate on Movie (but on other items)???
      m.setTitle(tmmMovie.getTitle());

      if (full) {
        // TODO: m.setDirectors();
        m.setDescription(tmmMovie.getPlot());
        m.setLanguage(tmmMovie.getSpokenLanguages());
        m.setRating(String.valueOf(tmmMovie.getRating()));

        List<String> genres = new ArrayList<>();
        for (MediaGenres g : tmmMovie.getGenres()) {
          genres.add(g.getLocalizedName());
        }
        if (!genres.isEmpty()) {
          String[] arr = genres.toArray(new String[genres.size()]);
          m.setGenres(arr);
        }

        List<PersonWithRole> persons = new ArrayList<>();
        for (MovieActor a : tmmMovie.getActors()) {
          persons.add(new PersonWithRole(a.getName(), a.getCharacter()));
        }
        if (!persons.isEmpty()) {
          PersonWithRole[] arr = persons.toArray(new PersonWithRole[persons.size()]);
          m.setActors(arr);
        }

        persons = new ArrayList<>();
        for (MovieProducer a : tmmMovie.getProducers()) {
          persons.add(new PersonWithRole(a.getName(), a.getCharacter()));
        }
        if (!persons.isEmpty()) {
          PersonWithRole[] arr = persons.toArray(new PersonWithRole[persons.size()]);
          m.setProducers(arr);
        }
      }

      for (MediaFile mf : tmmMovie.getMediaFiles()) {
        Res r = new Res(MimeTypes.getMimeType(mf.getExtension()), mf.getFilesize(),
            "http://" + IP + "/upnp/movies/" + tmmMovie.getDbId().toString() + "/" + mf.getFilename());
        m.addResource(r);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return m;
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
      throw new RuntimeException(e);
    }
  }
}
