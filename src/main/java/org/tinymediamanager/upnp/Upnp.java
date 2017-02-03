package org.tinymediamanager.upnp;

import java.io.IOException;
import java.util.ArrayList;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.registry.RegistrationException;
import org.fourthline.cling.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upnp {
  private static final Logger LOGGER      = LoggerFactory.getLogger(Upnp.class);
  private static Upnp         instance;
  private UpnpService         upnpService = null;
  private WebServer           webServer   = null;

  private Upnp() {
  }

  public synchronized static Upnp getInstance() {
    if (Upnp.instance == null) {
      Upnp.instance = new Upnp();
    }
    return Upnp.instance;
  }

  public UpnpService getUpnpService() {
    return this.upnpService;
  }

  private void createUpnpService() {
    if (upnpService == null) {
      this.upnpService = new UpnpServiceImpl(UpnpListener.getListener());
    }
  }

  public ArrayList<Device> getAvailablePlayers() {
    ArrayList<Device> ret = new ArrayList<>();
    for (Device device : upnpService.getRegistry().getDevices()) {
      if (device.getType().getType().equals("MediaRenderer")) {
        ret.add(device);
      }
    }
    return ret;
  }

  /**
   * starts a WebServer for accessing MediaFiles over UPNP<br>
   * In /upnp/(movie|tvshow)/UUIDofMediaEntity/(folder)/file.ext format
   */
  public void startWebServer() {
    try {
      if (this.webServer == null) {
        this.webServer = new WebServer();
      }
      else {
        LOGGER.warn("Cannot start webserver - already started!");
      }
    }
    catch (IOException e) {
      LOGGER.warn("Could not start WebServer!", e);
    }
  }

  public void stopWebServer() {
    if (this.webServer != null) {
      this.webServer.closeAllConnections();
    }
  }

  public void startMediaServer() {
    createUpnpService();
    try {
      this.upnpService.getRegistry().addDevice(MediaServer.createDevice());
    }
    catch (RegistrationException | LocalServiceBindingException | ValidationException | IOException e) {
      LOGGER.warn("could not start UPNP MediaServer!", e);
    }
  }

  public void stopMediaServer() {
    if (this.upnpService != null) {
      try {
        this.upnpService.getRouter().shutdown();
      }
      catch (RouterException e) {
        LOGGER.warn("Could not shutdown the UPNP router.");
      }
      this.upnpService.shutdown();
    }
  }

  public void shutdown() {
    stopWebServer();
    stopMediaServer();
  }
}
