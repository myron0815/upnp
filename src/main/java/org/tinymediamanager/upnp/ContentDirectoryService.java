package org.tinymediamanager.upnp;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.fourthline.cling.support.model.DIDLObject.Class;
import org.fourthline.cling.support.model.DIDLObject.Property.DC;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Movie;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.MovieActor;
import org.tinymediamanager.core.movie.entities.MovieProducer;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.scraper.entities.MediaGenres;
import org.tinymediamanager.thirdparty.NetworkUtil;

public class ContentDirectoryService extends AbstractContentDirectoryService {

  private static final String ID_ROOT         = "0";
  private static final String ID_MOVIES       = "1";
  private static final String ID_TVSHOWS      = "2";

  private static final Class  CONTAINER_CLASS = new Class("object.container");

  private static final String ip              = NetworkUtil.getMachineIPAddress();

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
        Container cont = new Container();
        cont.setId(ID_MOVIES);
        cont.setParentID(ID_ROOT);
        cont.setTitle("Movies");
        cont.setClazz(CONTAINER_CLASS);
        didl.addContainer(cont);

        cont = new Container();
        cont.setId(ID_TVSHOWS);
        cont.setParentID(ID_ROOT);
        cont.setTitle("TV Shows");
        cont.setClazz(CONTAINER_CLASS);
        didl.addContainer(cont);

        String ret = dip.generate(didl);
        System.out.println(prettyFormat(ret, 2));
        return new BrowseResult(ret, 2, 2);
      }

      if (browseFlag.equals(BrowseFlag.METADATA)) {
        // get specific objectID and ALL the metadata
        for (org.tinymediamanager.core.movie.entities.Movie m : MovieList.getInstance().getMovies()) {
          if (objectID.equals(m.getDbId().toString())) {
            didl.addItem(getUpnpMovie(m));
          }
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
      // just add basic things like title - no complete metadata needed (might be too slow)
      if (ID_MOVIES.equals(objectID)) {
        for (org.tinymediamanager.core.movie.entities.Movie m : MovieList.getInstance().getMovies()) {
          Movie u = new Movie(m.getDbId().toString(), ID_MOVIES, m.getTitle(), "?creator?", null);
          for (MediaFile mf : m.getMediaFiles()) {
            Res r = new Res(MimeTypes.getMimeType(mf.getExtension()), mf.getFilesize(), "http://" + ip + "/upnp/" + mf.getFilename());
            u.addResource(r);
          }
          didl.addItem(u);
        }
        count = didl.getItems().size();
      }
      else if (ID_TVSHOWS.equals(objectID)) {
        for (org.tinymediamanager.core.tvshow.entities.TvShow m : TvShowList.getInstance().getTvShows()) {
          // didl.addItem(getUpnpTvShow(m));
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

  private Movie getUpnpMovie(org.tinymediamanager.core.movie.entities.Movie tmmMovie) {

    System.out.println(tmmMovie.getTitle());
    Movie m = new Movie();
    try {
      m.setId(tmmMovie.getDbId().toString());
      m.setParentID(ID_MOVIES);

      m.addProperty(new DC.DATE(tmmMovie.getYear())); // no setDate on Movie (but on other items)???

      // TODO: m.setDirectors();
      m.setTitle(tmmMovie.getTitle());
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

      for (MediaFile mf : tmmMovie.getMediaFiles()) {
        Res r = new Res(MimeTypes.getMimeType(mf.getExtension()), mf.getFilesize(), "http://10.0.0.1/files/" + mf.getFilename());
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
