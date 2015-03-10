package oddymobstar.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oddymobstar.activity.DemoActivity;
import oddymobstar.core.Alliance;
import oddymobstar.core.Config;
import oddymobstar.core.Topic;
import oddymobstar.database.DBHelper;
import oddymobstar.message.in.Acknowledge;
import oddymobstar.message.in.AllianceMessage;
import oddymobstar.message.in.GridMessage;
import oddymobstar.message.in.TopicMessage;
import oddymobstar.message.out.CoreMessage;
import oddymobstar.message.out.PackageMessage;
import oddymobstar.util.Configuration;
import oddymobstar.util.UUIDGenerator;


/**
 * Created by root on 23/02/15.
 * <p/>
 * tjw decide if we want service or intent service.  technically its not threaded.
 * <p/>
 * but we do need to receive events but again these are on 1 socket.
 */
public class CheService extends IntentService {

    public static final int BUFFER_SIZE = 2048;
    /*
    we have 1 socket.  possibly configure to have more, but 1 is fine for testing
     */
    private Socket socket;
    private DataOutputStream dOut = null;
    private DataInputStream dIn = null;

    private Thread write;
    private Thread connect;
    private Thread read;
    private Thread locationUpdates;

    private static boolean ON_CONNECT = false;

    private List<CoreMessage> messageBuffer = new ArrayList<>();

    //service manager
    private LocationManager locationManager;
    private android.os.Handler handler = new android.os.Handler();


    private Map<String, CoreMessage> sentAcks = new HashMap<>();

    public class CheServiceBinder extends Binder {
        public CheService getCheServiceInstance() {
            return CheService.this;
        }
    }

    private CheServiceBinder cheServiceBinder = new CheServiceBinder();


    public class DemoLocationListener implements LocationListener {

        private UUIDGenerator uuidGenerator;

        /*
        this really needs to be moved to the service not the app......to review and test nearer time.  use away cambridge as test
         */

        private LocationManager locationManager;

        public DemoLocationListener(LocationManager locationManager) {
            this.locationManager = locationManager;
        }

        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            //  callBack.setLocationUpdated(location);
            uuidGenerator = new UUIDGenerator(configuration.getConfig(Configuration.UUID_ALGORITHM).getValue());
            try {
                CoreMessage coreMessage = new CoreMessage(new LatLng(location.getLatitude(), location.getLongitude()), configuration.getConfig(Configuration.PLAYER_KEY).getValue(), uuidGenerator.generateAcknowledgeKey(), CoreMessage.PLAYER);
                writeToSocket(coreMessage);

            }catch(NoSuchAlgorithmException nsae){
                Log.d("Security error", nsae.toString());
            }catch(JSONException jse){
                Log.d("JSON error", jse.toString());
            }

        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            locationManager.removeUpdates(this);

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
         //   locationManager.requestLocationUpdates(provider, Long.parseLong(configuration.getConfig(Configuration.GPS_UPDATE_INTERVAL).getValue()), 0, this);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    }

    private DemoLocationListener demoLocationListener;

    private Configuration configuration;
    private DBHelper dbHelper = new DBHelper(this);


    public CheService() {
        super("CheService");
    }


    @Override
    public void onCreate() {



        //this works fine for what i want (need to ensure it stays up.
        //also need to have timeout / reconnect etc.
        configuration = new Configuration(dbHelper.getConfigs());

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        demoLocationListener = new DemoLocationListener(locationManager);

        locationUpdates = new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Long.parseLong(configuration.getConfig(Configuration.GPS_UPDATE_INTERVAL).getValue()), 0, demoLocationListener);
                    }
                });

            }
        });

        locationUpdates.start();


        //st up our socket.
        connect = new Thread(new Runnable() {
            public void run() {
                connectSocket();
            }
        });

        connect.start();


    }

    @Override
    public IBinder onBind(Intent intent) {
        return cheServiceBinder;
    }

    @Override
    public void onDestroy() {


        super.onDestroy();

        if (dIn != null) {
            try {
                dIn.close();
            } catch (Exception e) {
                Log.d("dIn error", e.toString());
                dIn = null;
            }
        }

        if (dOut != null) {
            try {
                dOut.close();
            } catch (Exception e) {
                Log.d("dOut error", e.toString());
                dOut = null;
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                Log.d("socket error", e.toString());
                socket = null;
            }
        }

        locationUpdates = null;
        write = null;
        read = null;
        connect = null;


        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void socketListen() {


        try {


            byte[] buffer = new byte[BUFFER_SIZE];

            int charsRead = 0;

            while ((charsRead = dIn.read(buffer)) != -1) {
                try {
                    //each message we receive should be a JSON.  We need to work out the type.
                    oddymobstar.message.in.CoreMessage coreMessage = new oddymobstar.message.in.CoreMessage(new String(buffer).substring(0, charsRead));

                    Log.d("incoming", "the core message is " + coreMessage.getJsonObject().toString());
                    //at this point we could have more than 1 core message here.


                    //what are we?
                    if (!coreMessage.getJsonObject().isNull(Acknowledge.ACKNOWLEDGE)) {

                        Acknowledge ack = new Acknowledge(coreMessage.getJsonObject().getJSONObject(Acknowledge.ACKNOWLEDGE));
                        ack.create();

                            /*
                            acknowledges either tell us of a fail (we can log it etc) or tell us of a success and generally a UUID.
                           */

                        if (ack.getState().equals(Acknowledge.ERROR)) {
                            Log.d("ack error", "error information is " + ack.getInfo());
                        } else {
                            if (ack.getInfo().equals(Acknowledge.ACTIVE)) {
                                //we need to wake up our write thread.
                                if (write != null) {
                                    ON_CONNECT =true;

                                    Log.d("ack error", "trying to wake up thread");

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            synchronized (write) {
                                                write.notify();
                                            }
                                        }
                                    }).start();


                                }
                            }
                            //match to our acknowledge sent list...and this will hold the relevant action required.
                            CoreMessage ackSent = sentAcks.get(ack.getAckId());

                            if (ackSent != null) {
                                //is it a UUID message
                                if (ack.getInfo().equals(Acknowledge.UUID)) {
                                    if (ackSent instanceof oddymobstar.message.out.TopicMessage) {

                                        Topic topic = new Topic();
                                        topic.setKey(ack.getOid());
                                        topic.setName(ackSent.getMessage().getJSONObject(CoreMessage.CORE_OBJECT).getJSONObject(oddymobstar.message.out.TopicMessage.TOPIC).getString(oddymobstar.message.out.TopicMessage.TNAME));
                                        dbHelper.addTopic(topic);

                                    } else if (ackSent instanceof oddymobstar.message.out.AllianceMessage) {

                                        Alliance alliance = new Alliance();
                                        alliance.setKey(ack.getOid());
                                        alliance.setName(ackSent.getMessage().getJSONObject(CoreMessage.CORE_OBJECT).getJSONObject(oddymobstar.message.out.AllianceMessage.ALLIANCE).getString(oddymobstar.message.out.AllianceMessage.ANAME));
                                        dbHelper.addAlliance(alliance);

                                    } else if (ackSent instanceof PackageMessage) {

                                    } else {
                                        //its core.  therefore player.
                                        Config config = configuration.getConfig(Configuration.PLAYER_KEY);
                                        config.setValue(ack.getUid());
                                        dbHelper.updateConfig(config);
                                    }
                                }

                                  /*
                                   also we have the current UTM / SUBUTM so we need to update this too.

                                   */
                                if (!(ackSent instanceof oddymobstar.message.out.AllianceMessage
                                        || ackSent instanceof oddymobstar.message.out.TopicMessage
                                        || ackSent instanceof PackageMessage)) {
                                    Config config = configuration.getConfig(Configuration.CURRENT_UTM);
                                    config.setValue(ack.getUtm());
                                    dbHelper.updateConfig(config);

                                    config = configuration.getConfig(Configuration.CURRENT_SUBUTM);
                                    config.setValue(ack.getSubUtm());
                                    dbHelper.updateConfig(config);
                                }

                                sentAcks.remove(ack.getAckId());
                            }

                        }

                    } else if (!coreMessage.getJsonObject().isNull(GridMessage.GRID)) {

                        GridMessage gridMessage = new GridMessage(coreMessage.getJsonObject().getJSONObject(GridMessage.GRID));
                        gridMessage.create();

                    } else if (!coreMessage.getJsonObject().isNull(AllianceMessage.ALLIANCE)) {

                        AllianceMessage allianceMessage = new AllianceMessage(coreMessage.getJsonObject().getJSONObject(AllianceMessage.ALLIANCE));
                        allianceMessage.create();


                    } else if (!coreMessage.getJsonObject().isNull(TopicMessage.TOPIC)) {

                        TopicMessage topicMessage = new TopicMessage(coreMessage.getJsonObject().getJSONObject(TopicMessage.TOPIC));
                        topicMessage.create();

                        if (topicMessage.getFilter().equals(TopicMessage.POST)) {

                            try {
                                Topic topic = new Topic();
                                topic.setKey(topicMessage.getTid());
                                topic.setName(topicMessage.getTitle());
                                Topic checkGlobal = dbHelper.getGlobalTopic(topic.getKey());
                                if (!checkGlobal.getKey().equals(topic.getKey())) {
                                    dbHelper.addGlobalTopic(topic);
                                }
                                //this may not be catching it lol.  it seems not.  otherwise it all works (ie get global topics)
                            } catch (android.database.sqlite.SQLiteConstraintException e) {
                                Log.d("topic error", "sql exception  " + e.toString());
                            }
                        }

                    }

                  //  Log.d("socket listen", "we have read " + new String(buffer).substring(0, charsRead));

                } catch (JSONException jse) {
                    Log.d("json exception", "json exception " + jse.toString());
                }

            }


        } catch (Exception e) {

            Log.d("socket listen", "socket listen error " + e.toString());

        }


    }

    private void reConnect() {

        try {
            socket.close();
        } catch (Exception e) {

        }

        socket = null;
        connect = null;
        write = null;
        read = null;


        try {
            dIn.close();
        } catch (Exception e) {
        }
        try {
            dOut.close();
        } catch (Exception e) {
        }

        //try again
        connectSocket();


        //we now need to wait for the active connection message again.
        write = new Thread();

        write.start();


        synchronized (write) {
            try {
                write.wait();
            } catch (InterruptedException ie) {
                Log.d("wait", "wait " + ie.toString());
            } catch (Exception e) {
                Log.d("wait", "wait " + e.toString());
            }

            for(CoreMessage coreMessage : messageBuffer) {
                writeToSocket(coreMessage);
            }

            messageBuffer.clear();

        }


    }


    public void writeToSocket(final CoreMessage coreMessage) {



        try {

            sentAcks.put(coreMessage.getMessage().getJSONObject(CoreMessage.CORE_OBJECT).getString(CoreMessage.ACK_ID), coreMessage);
            dOut.writeUTF(coreMessage.getMessage().toString());


        } catch (Exception e) {
            Log.d("socket exception", "socket " + e.toString() + coreMessage.getMessage());
            ON_CONNECT = false;
            messageBuffer.add(coreMessage);

            connect = new Thread(new Runnable() {
                @Override
                public void run() {
                    reConnect();
                }
            });

            connect.start();

        }

    }

    /*
      this will also need to be used to reconnect the socket if it goes down or is shut
      (due to network signal etc).
     */
    private void connectSocket() {


        try {
            socket = new Socket(configuration.getConfig(Configuration.URL).getValue(), Integer.parseInt(configuration.getConfig(Configuration.PORT).getValue()));
            socket.setKeepAlive(true);


            //set up the two stream readers
            dOut = new DataOutputStream(socket.getOutputStream());
            dIn = new DataInputStream(socket.getInputStream());


            //now start listening to the socket this is ongoing.
            read = new Thread((new Runnable() {
                @Override
                public void run() {
                    socketListen();
                }
            }));

            read.start();


        } catch (Exception e) {
            //crashed.
            Log.d("socket error", "socket " + e.toString());


        }
    }

    @Override
    public ComponentName startService(Intent intent) {
        return super.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //to review this.
        return START_STICKY;
        //  return super.onStartCommand(intent,flags,startId);
    }


}
