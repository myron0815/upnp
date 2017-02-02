package org.tinymediamanager.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.entities.MediaFile;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Webserver extends NanoHTTPD {

  public Webserver() throws IOException {
    super(8008);
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    System.out.println("\nRunning! Point your browsers to http://localhost:8008/ \n");
  }

  public static void main(String[] args) {
    try {
      new Webserver();
    }
    catch (IOException ioe) {
      System.err.println("Couldn't start server:\n" + ioe);
    }
  }

  @Override
  public Response serve(IHTTPSession session) {
    String uri = session.getUri();
    if (uri.startsWith("/upnp")) {
      String[] path = StringUtils.split(uri);
      // [0] = upnp
      // [1] = movie|tvshow
      // [2] = UUID of MediaEntity
      // [3] = MF relative path

      MediaFile mf = new MediaFile();
      return serveFile(session.getHeaders(), mf);
    }

    return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "BAD REQUEST");
  }

  // CLONE from nanohttp-webserver (supporting ranges)
  // reworked for NIO Path and MF access
  Response serveFile(Map<String, String> header, MediaFile file) {
    Response res;
    try {
      String mime = MimeTypes.getMimeTypeAsString(file.getExtension());

      // Calculate etag
      String etag = Integer
          .toHexString((file.getFileAsPath().toString() + Files.getLastModifiedTime(file.getFileAsPath()) + "" + file.getFilesize()).hashCode());

      // Support (simple) skipping:
      long startFrom = 0;
      long endAt = -1;
      String range = header.get("range");
      if (range != null) {
        if (range.startsWith("bytes=")) {
          range = range.substring("bytes=".length());
          int minus = range.indexOf('-');
          try {
            if (minus > 0) {
              startFrom = Long.parseLong(range.substring(0, minus));
              endAt = Long.parseLong(range.substring(minus + 1));
            }
          }
          catch (NumberFormatException ignored) {
          }
        }
      }

      // get if-range header. If present, it must match etag or else we
      // should ignore the range request
      String ifRange = header.get("if-range");
      boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

      String ifNoneMatch = header.get("if-none-match");
      boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

      // Change return code and add Content-Range header when skipping is
      // requested
      long fileLen = file.getFilesize();

      if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
        // range request that matches current etag
        // and the startFrom of the range is satisfiable
        if (headerIfNoneMatchPresentAndMatching) {
          // range request that matches current etag
          // and the startFrom of the range is satisfiable
          // would return range from file
          // respond with not-modified
          res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
          res.addHeader("ETag", etag);
        }
        else {
          if (endAt < 0) {
            endAt = fileLen - 1;
          }
          long newLen = endAt - startFrom + 1;
          if (newLen < 0) {
            newLen = 0;
          }

          InputStream fis = Files.newInputStream(file.getFileAsPath());
          fis.skip(startFrom);

          res = newFixedLengthResponse(Status.PARTIAL_CONTENT, mime, fis, newLen);
          res.addHeader("Accept-Ranges", "bytes");
          res.addHeader("Content-Length", "" + newLen);
          res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
          res.addHeader("ETag", etag);
        }
      }
      else {

        if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
          // return the size of the file
          // 4xx responses are not trumped by if-none-match
          res = newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
          res.addHeader("Content-Range", "bytes */" + fileLen);
          res.addHeader("ETag", etag);
        }
        else if (range == null && headerIfNoneMatchPresentAndMatching) {
          // full-file-fetch request
          // would return entire file
          // respond with not-modified
          res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
          res.addHeader("ETag", etag);
        }
        else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
          // range request that doesn't match current etag
          // would return entire (different) file
          // respond with not-modified

          res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
          res.addHeader("ETag", etag);
        }
        else {
          // supply the file
          res = newFixedLengthResponse(Response.Status.OK, mime, Files.newInputStream(file.getFileAsPath()), (int) file.getFilesize());
          res.addHeader("Accept-Ranges", "bytes");
          res.addHeader("Content-Length", "" + fileLen);
          res.addHeader("ETag", etag);
        }
      }
    }
    catch (IOException ioe) {
      res = newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
    }

    return res;
  }
}
