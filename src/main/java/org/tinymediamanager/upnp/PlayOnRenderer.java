package org.tinymediamanager.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.InvalidValueException;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.item.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaFile;

public class PlayOnRenderer {
  private static final Logger LOGGER  = LoggerFactory.getLogger(PlayOnRenderer.class);

  Service                     service = null;

  public PlayOnRenderer(Service service) {
    this.service = service;
  }

  private void play(Movie m, MediaFile mf) {
    String meta = getMeta(m);
    ActionCallback setAVTransportURIAction = new SetAVTransportURI(service, mf.getFileAsPath().toString(), meta) {
      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        LOGGER.warn("Error playing MF: " + defaultMsg);
      }
    };
  }

  private String getMeta(Movie m) {
    DIDLContent didl = new DIDLContent();
    DIDLParser dip = new DIDLParser();
    didl.addItem(m);
    try {
      return dip.generate(didl);
    }
    catch (Exception e) {
      return null;
    }
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
