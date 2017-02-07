package org.tinymediamanager.upnp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.fourthline.cling.model.meta.Device;
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
    u.createUpnpService();
    u.sendPlayerSearchRequest();
    u.startWebServer();
    u.startMediaServer();

    // shutdown
    LOGGER.info("Press enter to exit");
    String s = "dummy";
    while (!s.isEmpty()) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      s = br.readLine();

      if ("play".equals(s)) {
        u.setPlayer(u.getAvailablePlayers().get(0));
        u.playFile("http://1.2.3.4:8008/upnp/movies/68bcb1d0-cc3f-440a-8de2-8eb82fb7cac5/file.mp4");
      }

      if ("stop".equals(s)) {
        u.stopPlay();
      }

      if ("list".equals(s)) {
        LOGGER.info("=== list devices START ===");
        for (Device d : u.getUpnpService().getRegistry().getDevices()) {
          LOGGER.info(d.getDisplayString() + " (" + d.getType().getType() + ")");
        }
        LOGGER.info("=== list devices END ===");
      }

    }

    u.shutdown();
  }

}
