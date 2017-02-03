package org.tinymediamanager.upnp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    // load TMM database
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    Upnp u = Upnp.getInstance();
    u.startWebServer();
    u.startMediaServer();

    // shutdown
    LOGGER.info("Press enter to exit");
    String s = "dummy";
    while (!s.isEmpty()) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      s = br.readLine();
    }

    u.shutdown();
  }

}
