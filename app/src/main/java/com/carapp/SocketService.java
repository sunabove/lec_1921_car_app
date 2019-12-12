package com.carapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SocketService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_CURR_LOC = "com.carapp.action.ACTION_CURR_LOC";
    public static final String ACTION_BAZ = "com.carapp.action.ACTION_BAZ";

    // TODO: Rename parameters
    public static final String EXTRA_PARAM1 = "com.carapp.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "com.carapp.extra.PARAM2";

    private Socket socket ;

    public SocketService() {
        super("SocketService");
    }

    public Socket getSocket() {
        if( null != this.socket ) {
            return this.socket ;
        } else if( null == this.socket ) {
            try {
                this.socket = IO.socket("http://10.3.141.1");

                return this.socket ;
            } catch (Exception e) {
                e.printStackTrace();
                this.socket = null;
            }
        }

        return this.socket ;
    }

    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SocketService.class);
        intent.setAction(ACTION_CURR_LOC);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SocketService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CURR_LOC.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionCurrLoc(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

    private int handleCnt = 0 ;
    private String tag = "socket";

    private void handleActionCurrLoc(String param1, String param2) {
        // TODO: Handle action Foo

        Log.d( tag, "handle count = " + handleCnt );

        handleCnt += 1;

        this.socket = this.getSocket();

        //final Activity_03_Map activity = this;
        final String tag = "socket" ;

        if( null != socket ) {
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d( tag, "socket connected");
                    socket.emit("send_me_curr_pos" );
                }

            });

            socket.on("send_me_curr_pos", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    if( null == args ) {
                        Log.d( tag, "argument is null.");
                    } else {
                        int idx = 0;
                        for (Object arg : args) {
                            Log.d(tag, String.format("[%03d] curr_pos args = %s", idx, "" + arg));
                            idx += 1;
                        }
                    }

                    socket.emit("send_me_curr_pos" );
                }

            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {

                    SocketService.this.socket = null ;

                    Log.d( tag, "socket disconnected.");
                }

            });

            if( ! socket.connected() ) {
                socket.connect();
            } else {
            }
        }
    }

    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
