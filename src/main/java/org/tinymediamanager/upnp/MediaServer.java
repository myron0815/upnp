package org.tinymediamanager.upnp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.MovieModuleManager;

public class MediaServer implements Runnable {

  public static void main(String[] args) throws Exception {
    // Start a user thread that runs the UPnP stack
    Thread serverThread = new Thread(new MediaServer());
    serverThread.setDaemon(false);
    serverThread.start();
  }

  public void run() {
    try {
      TmmModuleManager.getInstance().startUp();
      MovieModuleManager.getInstance().startUp();

      final UpnpService upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration());

      // Add the bound local device to the registry
      upnpService.getRegistry().addDevice(createDevice());

      System.out.println("Press enter to exit");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String s = br.readLine();
      upnpService.getRouter().shutdown();
      upnpService.shutdown();
    }
    catch (Exception ex) {
      System.err.println("Exception occured: " + ex);
      ex.printStackTrace(System.err);
      System.exit(1);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static LocalDevice createDevice() throws ValidationException, LocalServiceBindingException, IOException {
    DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("tinyMediaManager"));
    DeviceType type = new UDADeviceType("MediaServer", 1);
    DeviceDetails details = new DeviceDetails("Test", new ManufacturerDetails("tinyMediaManager", "http://tinymediamanager.org/"),
        new ModelDetails("BinLight2000", "A demo light with on/off switch.", "v1"));

    System.out.println("Hello, i'm " + identity.getUdn().getIdentifierString());

    // Content Directory Service
    LocalService cds = new AnnotationLocalServiceBinder().read(ContentDirectoryService.class);
    cds.setManager(new DefaultServiceManager<ContentDirectoryService>(cds, ContentDirectoryService.class));

    // Connection Manager Service
    LocalService<ConnectionManagerService> cms = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
    cms.setManager(new DefaultServiceManager<>(cms, ConnectionManagerService.class));

    return new LocalDevice(identity, type, details, new LocalService[] { cds, cms });

  }
}
