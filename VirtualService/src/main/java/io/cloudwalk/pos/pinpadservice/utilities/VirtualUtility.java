package io.cloudwalk.pos.pinpadservice.utilities;

import static java.util.Locale.US;
import static io.cloudwalk.pos.pinpadlibrary.IServiceCallback.NTF;
import static io.cloudwalk.pos.pinpadlibrary.IServiceCallback.NTF_MSG;
import static io.cloudwalk.pos.pinpadlibrary.IServiceCallback.NTF_TYPE;

import android.os.Bundle;
import android.os.NetworkOnMainThreadException;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import io.cloudwalk.loglibrary.Log;
import io.cloudwalk.pos.pinpadlibrary.ABECS;
import io.cloudwalk.pos.pinpadlibrary.internals.utilities.PinpadUtility;

public class VirtualUtility {
    private static final String
            TAG = VirtualUtility.class.getSimpleName();

    private static Socket
            sServerSocket = null;

    public static final BlockingQueue<Bundle>
            sResponseQueue = new LinkedBlockingQueue<>();

    public static final Semaphore
            sVirtualSemaphore = new Semaphore(1, true);

    private VirtualUtility() {
        Log.d(TAG, "VirtualUtility");
    }

    private static byte[] _intercept(String action, byte[] stream) {
        // Log.d(TAG, "_intercept");

        try {
            if (stream.length < 3) {
                return stream;
            }

            switch (action) {
                case "recv":
                    Bundle response = PinpadUtility.parseResponseDataPacket(stream, stream.length);

                    switch (response.getString(ABECS.RSP_ID, "UNKNOWN")) {
                        case ABECS.GIX:
                            if (!response.containsKey(ABECS.PP_MODEL)) {
                                break;
                            }

                            response.putString(ABECS.PP_MODEL, String.format(US, "%.20s", "VIRTUAL/" + response.getString(ABECS.PP_MODEL)));

                            Log.d(TAG, ABECS.PP_MODEL + "[" + response.getString(ABECS.PP_MODEL) + "]");

                            // TODO: return PinpadUtility.buildResponseDataPacket(response);

                        default:
                            /* Nothing to do */
                            break;
                    }

                default:
                    /* Nothing to do */
                    break;
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));
        }

        return stream;
    }

    private static int _route(Bundle bundle)
            throws Exception {
        // Log.d(TAG, "_route");

        String address  = SharedPreferencesUtility.readIPv4();
        int    delim    = address.indexOf(":");

        String host     = address.substring(0, delim);
        int    port     = Integer.parseInt(address.substring(delim + 1));

        byte[] request  = bundle.getByteArray("request");

        if (address.contains("127.0.0.1") || address.contains("0.0.0.0")) {
            // TODO: return MockUtility.send(bundle);
        }

        if (sServerSocket != null
                && sServerSocket.isConnected() && !sServerSocket.isClosed()) {
            sServerSocket.close();

            Log.d(TAG, "_route::" + sServerSocket + " (close) (overlapping)");
        }

        sServerSocket = new Socket();

        sServerSocket.setPerformancePreferences(2, 1, 0);

        sServerSocket.connect(new InetSocketAddress(host, port), 2000);

        sServerSocket.getOutputStream().write(request);
        sServerSocket.getOutputStream().flush();

        new Thread() {
            @Override
            public void run() {
                super.run();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Socket socket = sServerSocket;

                try {
                    sVirtualSemaphore.acquireUninterruptibly();

                    while (true) {
                        if (sResponseQueue.poll() == null) { break; }
                    }

                    byte[] buffer = new byte[2048];
                    int    count  = 0;

                    do {
                        socket.setSoTimeout((count != 0) ? 0 : 2000);

                        count = socket.getInputStream().read(buffer, 0, buffer.length);

                        if (count >= 0) {
                            stream.write(buffer, 0, count);

                            Bundle response = new Bundle();

                            String applicationId = bundle.getString("application_id");

                            response.putString   ("application_id", applicationId);
                            response.putByteArray("response",       _intercept("recv", stream.toByteArray()));

                            stream.reset();

                            sResponseQueue.add(response);

                            if (buffer[0] != 0x06) { return; }

                            try {
                                String CMD_ID = bundle.getBundle("request_bundle").getString(ABECS.CMD_ID);

                                Bundle callback = new Bundle();

                                callback.putString(NTF_MSG,  String.format(US, "\nPROCESSING %s\n/%s", CMD_ID, socket.getInetAddress().getHostAddress()));
                                callback.putInt   (NTF_TYPE, NTF);

                                CallbackUtility.getServiceCallback().onServiceCallback(callback);
                            } catch (NullPointerException exception) {
                                // 2022-01-25: nothing to do: probably a control byte
                            }
                        }
                    } while (count <= 1);
                } catch (Exception exception) {
                    Log.e(TAG, Log.getStackTraceString(exception));
                } finally {
                    try { socket.close(); } catch (Exception exception) { Log.e(TAG, Log.getStackTraceString(exception)); }

                    sVirtualSemaphore.release();
                }
            }
        }.start();

        return request.length;
    }

    public static int abort(Bundle bundle) {
        Log.d(TAG, "abort");

        return send(bundle);
    }

    public static int send(Bundle bundle) {
        Log.d(TAG, "send");

        int[] status = { -1 };

        try {
            return _route(bundle);  // 2021-01-25: in theory, none should consume the service in
                                    // the main thread
        } catch (NetworkOnMainThreadException exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            Semaphore semaphore = new Semaphore(0, true);

            new Thread() {          // 2021-01-25: bypass `NetworkOnMainThreadException` for those
                @Override           // consuming the service in the main thread
                public void run() {
                    super.run();

                    try {
                        status[0] = _route(bundle);
                    } catch (Exception exception) {
                        Log.e(TAG, Log.getStackTraceString(exception));
                    } finally {
                        semaphore.release();
                    }
                }
            }.start();

            semaphore.acquireUninterruptibly();

            return status[0];
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));
        }

        return -1;
    }
}