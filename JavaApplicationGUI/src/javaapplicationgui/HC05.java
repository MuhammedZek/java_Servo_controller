package javaapplicationgui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

public class HC05 {

    boolean scanFinished = false;
    RemoteDevice hc05device = null;
    String hc05Url;

    Vector<Device> devices = new Vector<>();

    public void scan() throws Exception {
        // 1
        LocalDevice localDevice = LocalDevice.getLocalDevice();

        // 2
        DiscoveryAgent agent = localDevice.getDiscoveryAgent();
        
        //3
        agent.startInquiry(DiscoveryAgent.GIAC, new DiscoveryListener() {
            @Override
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                try {
                    String name = btDevice.getFriendlyName(false);
                    RemoteDevice dev = btDevice;
                    Device d = new Device(name, dev);
                    devices.add(d);
                    System.out.println(name + " added");

                } catch (IOException ex) {
                    Logger.getLogger(HC05.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void servicesDiscovered(int i, ServiceRecord[] srs) {
                scanFinished = true;
            }

            @Override
            public void serviceSearchCompleted(int i, int i1) {

            }

            @Override
            public void inquiryCompleted(int i) {
                System.out.println("search is complet");
            }

        });
    }

    public void conect(String s) throws Exception {
        System.out.println(s);
        if ("HC-05".equals(s)) {
            System.out.println("connecting");
            for (int i = 0; i < devices.size(); i++) {
                if ("HC-05".equals(devices.elementAt(i).name)) {
                    hc05device = devices.elementAt(i).hc05device;
                }

            }
            UUID uuid = new UUID(0x1101); //scan for btspp://... services (as HC-05 offers it)
            UUID[] searchUuidSet = new UUID[]{uuid};
            int[] attrIDs = new int[]{
                0x0100 // service name
            };
            scanFinished = false;
            LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet,
                    hc05device, new DiscoveryListener() {
                        @Override
                        public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                        }

                        @Override
                        public void inquiryCompleted(int discType) {
                        }

                        @Override
                        public void serviceSearchCompleted(int transID, int respCode) {
                            scanFinished = true;
                        }

                        @Override
                        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                            for (int i = 0; i < servRecord.length; i++) {
                                hc05Url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                                if (hc05Url != null) {
                                    break; //take the first one
                                }
                            }
                        }
                    });

            while (!scanFinished) {
                Thread.sleep(500);
            }
            
            System.out.println("got it");
        } else {
            System.out.println("cannot conect");

        }

    }

    public void rotat(String s) throws Exception {
        if (hc05device != null) {
            StreamConnection streamConnection = (StreamConnection) Connector.open(hc05Url);
            OutputStream os = streamConnection.openOutputStream();
            InputStream is = streamConnection.openInputStream();

            os.write(s.getBytes());
            os.close();
            is.close();
            streamConnection.close();
        } else {
            System.out.println("connect first");
        }
    }

    public void sendMessageToDevice() {
        try {
            System.out.println("Connecting to " + hc05Url);

            ClientSession clientSession = (ClientSession) Connector.open(hc05Url);
            HeaderSet hsConnectReply = clientSession.connect(null);
            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                System.out.println("Failed to connect");
                return;
            }

            HeaderSet hsOperation = clientSession.createHeaderSet();
            hsOperation.setHeader(HeaderSet.NAME, "action.txt");
            hsOperation.setHeader(HeaderSet.TYPE, "text");

            //Create PUT Operation
            Operation putOperation = clientSession.put(hsOperation);

            // Sending the message
            byte data[] = "rotat 90 grad!".getBytes("iso-8859-1");
            OutputStream os = putOperation.openOutputStream();
            os.write(data);
            os.close();

            putOperation.close();
            clientSession.disconnect(null);
            clientSession.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
