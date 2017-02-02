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

    // start WebServer
    WebServer ws = new WebServer();

    // start MediaServer
    MediaServer ms = new MediaServer();
    Thread upnpThread = new Thread(ms);
    upnpThread.setDaemon(false);
    upnpThread.start();

    // shutdown
    LOGGER.info("Press enter to exit");
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String s = br.readLine();
    ms.upnpService.getRouter().shutdown();
    ms.upnpService.shutdown();
    ws.closeAllConnections();
  }

}
