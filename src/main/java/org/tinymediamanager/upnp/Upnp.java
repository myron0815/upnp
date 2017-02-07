package org.tinymediamanager.upnp;

import java.io.IOException;
import java.util.ArrayList;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.RegistrationException;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upnp {
  private static final Logger LOGGER        = LoggerFactory.getLogger(Upnp.class);
  private static Upnp         instance;
  private UpnpService         upnpService   = null;
  private WebServer           webServer     = null;
  private Service             playerService = null;

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

  /**
   * Starts out UPNP Service / Listener
   */
  public void createUpnpService() {
    if (upnpService == null) {
      this.upnpService = new UpnpServiceImpl(UpnpListener.getListener());
    }
  }

  /**
   * Sends a UPNP broadcast message, to find some players.<br>
   * Should be available shortly via getAvailablePlayers()
   */
  public void sendPlayerSearchRequest() {
    upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("MediaRenderer")));
  }

  /**
   * Finds all available Players (implementing the MediaRenderer stack)<br>
   * You might want to call sendPlayerSearchRequest() a few secs before, to populate freshly
   * 
   * @return List of devices
   */
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
   * Sets a device as our player for play/stop and other services
   * 
   * @param device
   *          device for playing
   */
  public void setPlayer(Device device) {
    this.playerService = device.findService(new UDAServiceId("AVTransport"));
    if (this.playerService == null) {
      LOGGER.warn("Could not find AVTransportservice on device " + device.getDisplayString());
    }
  }

  /**
   * Plays a file/url
   * 
   * @param url
   */
  public void playFile(String url) {
    if (this.playerService == null) {
      LOGGER.warn("No player set - did you call setPlayer() ?");
      return;
    }

    ActionCallback setAVTransportURIAction = new SetAVTransportURI(this.playerService, url, "NO METADATA") {
      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        LOGGER.warn("Setting URL for player failed! " + defaultMsg);
      }
    };
    upnpService.getControlPoint().execute(setAVTransportURIAction);

    ActionCallback playAction = new Play(this.playerService) {
      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        LOGGER.warn("Playing failed! " + defaultMsg);
      }
    };
    upnpService.getControlPoint().execute(playAction);
  }

  /**
   * stop the player
   */
  public void stopPlay() {
    if (this.playerService == null) {
      LOGGER.debug("No player set - no need to stop");
      return;
    }

    ActionCallback stopAction = new Stop(this.playerService) {
      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        LOGGER.warn("Stopping failed! " + defaultMsg);
      }
    };
    upnpService.getControlPoint().execute(stopAction);

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
    stopPlay();
    stopWebServer();
    stopMediaServer();
  }
}
