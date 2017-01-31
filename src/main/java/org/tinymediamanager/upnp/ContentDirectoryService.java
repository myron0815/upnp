package org.tinymediamanager.upnp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
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

public class ContentDirectoryService extends AbstractContentDirectoryService {

	private static final String ID_MOVIES = "movies";
	private static final String ID_TVSHOWS = "tvshows";

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults,
			SortCriterion[] orderby) throws ContentDirectoryException {
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

			if (objectID.equals("0") && browseFlag.equals(BrowseFlag.METADATA)) {
				// ???
				StorageFolder cont = new StorageFolder();
				cont.setId(ID_MOVIES);
				cont.setTitle("Movies");
				cont.setParentID("0");
				didl.addContainer(cont);
				return new BrowseResult(new DIDLParser().generate(didl), 1, 1);
			} else if (objectID.equals("0") && browseFlag.equals(BrowseFlag.DIRECT_CHILDREN)) {
				StorageFolder cont = new StorageFolder();
				cont.setId(ID_MOVIES);
				cont.setTitle("Movies");
				cont.setParentID("0");
				didl.addContainer(cont);

				cont = new StorageFolder();
				cont.setId(ID_TVSHOWS);
				cont.setTitle("TV Shows");
				cont.setParentID("0");
				didl.addContainer(cont);
				return new BrowseResult(new DIDLParser().generate(didl), 2, 2);
			}

			int count = 0;
			if (ID_MOVIES.equals(objectID)) {
				for (org.tinymediamanager.core.movie.entities.Movie m : MovieList.getInstance().getMovies()) {
					didl.addItem(getUpnpMovie(m));
				}
				count = MovieList.getInstance().getMovieCount();
			} else if (ID_TVSHOWS.equals(objectID)) {
				for (org.tinymediamanager.core.tvshow.entities.TvShow m : TvShowList.getInstance().getTvShows()) {
					// didl.addItem(getUpnpTvShow(m));
				}
				count = TvShowList.getInstance().getTvShowCount();
			} else {
				// ???
			}

			return new BrowseResult(new DIDLParser().generate(didl), count, count);

		} catch (Exception ex) {
			throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
		}
	}

	@Override
	public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult,
			long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
		// You can override this method to implement searching!
		return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
	}

	private Movie getUpnpMovie(org.tinymediamanager.core.movie.entities.Movie tmmMovie) {

		System.out.println(tmmMovie.getTitle());
		Movie m = new Movie();
		try {
			m.setId(tmmMovie.getDbId().toString());
			m.setCreator("tmm");
			m.setParentID(ID_MOVIES);

			// m.setDirectors();
			m.setTitle(tmmMovie.getTitle());
			m.setDescription(tmmMovie.getPlot());

			List<String> genres = new ArrayList<>();
			for (MediaGenres g : tmmMovie.getGenres()) {
				genres.add(g.getLocalizedName());
			}
			if (!genres.isEmpty()) {
				String[] arr = genres.toArray(new String[genres.size()]);
				m.setGenres(arr);
			}

			m.setLanguage(tmmMovie.getSpokenLanguages());
			m.setRating(String.valueOf(tmmMovie.getRating()));

			List<PersonWithRole> persons = new ArrayList<PersonWithRole>();
			for (MovieActor a : tmmMovie.getActors()) {
				persons.add(new PersonWithRole(a.getName(), a.getCharacter()));
			}
			if (!persons.isEmpty()) {
				PersonWithRole[] arr = persons.toArray(new PersonWithRole[persons.size()]);
				m.setActors(arr);
			}

			persons = new ArrayList<PersonWithRole>();
			for (MovieProducer a : tmmMovie.getProducers()) {
				persons.add(new PersonWithRole(a.getName(), a.getCharacter()));
			}
			if (!persons.isEmpty()) {
				PersonWithRole[] arr = persons.toArray(new PersonWithRole[persons.size()]);
				m.setProducers(arr);
			}

			for (MediaFile mf : tmmMovie.getMediaFiles()) {
				Res r = new Res(MimeTypes.getMimeType(mf.getExtension()), mf.getFilesize(),
						"http://10.0.0.1/files/" + mf.getFilename());
				m.addResource(r);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return m;
	}
}
