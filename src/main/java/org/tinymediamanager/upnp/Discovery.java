package org.tinymediamanager.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Discovery {
  private static final Logger LOGGER = LoggerFactory.getLogger(Discovery.class);

  public static void main(String[] args) throws Exception {

    // UPnP discovery is asynchronous, we need a callback
    RegistryListener listener = new RegistryListener() {

      public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
        LOGGER.debug("Discovery started: " + device.getDisplayString());
      }

      public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
        LOGGER.debug("Discovery failed: " + device.getDisplayString() + " => " + ex);
      }

      public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        LOGGER.debug("Remote device available: " + device.getDisplayString());
        if (!device.getType().getType().equals("MediaRenderer")) {
          // some respond, although they're not our search type
          LOGGER.debug("...but not our type - remove");
          registry.removeDevice(device);
        }
      }

      public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        // LOGGER.debug("Remote device updated: " + device.getDisplayString());
      }

      public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        LOGGER.debug("Remote device removed: " + device.getDisplayString());
      }

      public void localDeviceAdded(Registry registry, LocalDevice device) {
        LOGGER.debug("Local device added: " + device.getDisplayString());
      }

      public void localDeviceRemoved(Registry registry, LocalDevice device) {
        LOGGER.debug("Local device removed: " + device.getDisplayString());
      }

      public void beforeShutdown(Registry registry) {
        LOGGER.debug("Before shutdown, the registry has devices: " + registry.getDevices().size());
      }

      public void afterShutdown() {
        LOGGER.debug("Shutdown of registry complete!");

      }
    };

    // This will create necessary network resources for UPnP right away
    LOGGER.info("Starting Cling...");
    UpnpService upnpService = new UpnpServiceImpl(listener);

    // Send a search message to all devices and services, they should respond soon
    upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("MediaRenderer")));
    // Let's wait 10 seconds for them to respond
    LOGGER.debug("Waiting 10 seconds before shutting down...");
    Thread.sleep(10000);

    for (Device d : upnpService.getRegistry().getDevices()) {
      LOGGER.debug(d.toString());
    }

    // Release all resources and advertise BYEBYE to other UPnP devices
    LOGGER.info("Stopping Cling...");
    upnpService.shutdown();
  }

}
