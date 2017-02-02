package org.tinymediamanager.upnp;

import java.io.IOException;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.InvalidValueException;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer implements Runnable {
  private static final Logger LOGGER      = LoggerFactory.getLogger(MediaServer.class);

  final UpnpService           upnpService = new UpnpServiceImpl();

  public void run() {
    try {
      // MediaServer
      upnpService.getRegistry().addDevice(createDevice());

      // ControlPoint
      upnpService.getRegistry().addListener(createRegistryListener(upnpService));
      upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("MediaRenderer", 1)));

    }
    catch (Exception ex) {
      LOGGER.error("Exception occured: ", ex);
      System.exit(1);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static LocalDevice createDevice() throws ValidationException, LocalServiceBindingException, IOException {
    DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("tinyMediaManager"));
    DeviceType type = new UDADeviceType("MediaServer", 1);
    DeviceDetails details = new DeviceDetails("Test", new ManufacturerDetails("tinyMediaManager", "http://tinymediamanager.org/"),
        new ModelDetails("BinLight2000", "A demo light with on/off switch.", "v1"));

    LOGGER.info("Hello, i'm " + identity.getUdn().getIdentifierString());

    // Content Directory Service
    LocalService cds = new AnnotationLocalServiceBinder().read(ContentDirectoryService.class);
    cds.setManager(new DefaultServiceManager<ContentDirectoryService>(cds, ContentDirectoryService.class));

    // Connection Manager Service
    LocalService<ConnectionManagerService> cms = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
    cms.setManager(new DefaultServiceManager<>(cms, ConnectionManagerService.class));

    return new LocalDevice(identity, type, details, new LocalService[] { cds, cms });
  }

  private RegistryListener createRegistryListener(final UpnpService upnpService) {
    return new DefaultRegistryListener() {
      ServiceId serviceId = new UDAServiceId("SwitchPower");

      @Override
      public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        System.out.println("added: " + device);
        Service switchPower;
        if ((switchPower = device.findService(serviceId)) != null) {
          System.out.println("Service discovered: " + switchPower);
          executeAction(upnpService, switchPower);
        }
      }

      @Override
      public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        System.out.println("removed: " + device);
        Service switchPower;
        if ((switchPower = device.findService(serviceId)) != null) {
          System.out.println("Service disappeared: " + switchPower);
        }
      }
    };
  }

  private void executeAction(UpnpService upnpService, Service switchPowerService) {
    ActionInvocation setTargetInvocation = new SetTargetActionInvocation(switchPowerService);
    // Executes asynchronous in the background
    upnpService.getControlPoint().execute(new ActionCallback(setTargetInvocation) {
      @Override
      public void success(ActionInvocation invocation) {
        assert invocation.getOutput().length == 0;
        System.out.println("Successfully called action!");
      }

      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        System.err.println(defaultMsg);
      }
    });
  }

  class SetTargetActionInvocation extends ActionInvocation {
    SetTargetActionInvocation(Service service) {
      super(service.getAction("SetTarget"));
      try {
        // Throws InvalidValueException if the value is of wrong type
        setInput("NewTargetValue", true);
      }
      catch (InvalidValueException ex) {
        System.err.println(ex.getMessage());
        System.exit(1);
      }
    }
  }

}
