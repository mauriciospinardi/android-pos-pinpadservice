package io.cloudwalk.pos.pinpadlibrary.managers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import io.cloudwalk.loglibrary.Log;
import io.cloudwalk.pos.pinpadlibrary.ABECS;
import io.cloudwalk.pos.pinpadlibrary.IPinpadService;
import io.cloudwalk.pos.pinpadlibrary.IServiceCallback;
import io.cloudwalk.pos.pinpadlibrary.internals.utilities.PinpadUtility;
import io.cloudwalk.utilitieslibrary.Application;
import io.cloudwalk.utilitieslibrary.utilities.ServiceUtility;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PinpadManager {
    private static final String
            TAG = IPinpadService.class.getSimpleName().substring(1);

    private static final Semaphore
            sExchangeSemaphore = new Semaphore(1, true);

    private static final Semaphore
            sReceiveSemaphore  = new Semaphore(1, true);

    private static final Semaphore
            sRequestSemaphore  = new Semaphore(1, true);

    private static final String
            ACTION_PINPAD_SERVICE  = "io.cloudwalk.pos.pinpadservice.PinpadService";

    private static final String
            PACKAGE_PINPAD_SERVICE = "io.cloudwalk.pos.pinpadservice";

    private static int receive(Bundle bundle, long timeout) {
        Log.d(TAG, "receive");

        long timestamp = SystemClock.elapsedRealtime();

        byte[] response = new byte[2048];
        int    result  = -2;

        try {
            if (!sReceiveSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
                return result;
            }

            try {
                bundle.putLong("timeout", timeout);

                IBinder binder = ServiceUtility.retrieve(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE);

                result = IPinpadService.Stub.asInterface(binder)
                                .getPinpadManager(null).recv(bundle);

                response = bundle.getByteArray("response");
            } finally {
                sReceiveSemaphore.release();
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            result = -1;
        } finally {
            Log.h(TAG, response, result);

            Log.d(TAG, "receive::timestamp [" + (SystemClock.elapsedRealtime() - timestamp) + "]");
        }

        return result;
    }

    private static int send(Bundle bundle, IServiceCallback callback) {
        Log.d(TAG, "send");

        long timestamp = SystemClock.elapsedRealtime();
        int  result    = 0;

        byte[] request = bundle.getByteArray("request");

        Log.h(TAG, request, request.length);

        try {
            bundle.putString("application_id", Application.getPackageContext().getPackageName());

            IBinder binder = ServiceUtility.retrieve(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE);

            result = IPinpadService.Stub.asInterface(binder)
                    .getPinpadManager(null).send(bundle, callback);
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            result = -1;
        } finally {
            Log.d(TAG, "send::timestamp [" + (SystemClock.elapsedRealtime() - timestamp) + "]");
        }

        return result;
    }

    /**
     * Constructor.
     */
    private PinpadManager() {
        Log.d(TAG, "PinpadManager");

        /* Nothing to do */
    }

    /**
     * Performs an ABECS PINPAD request using the key/value format, heavily based on the default
     * public data format, as specified by the ABECS PINPAD protocol.
     *
     * @param bundle {@link Bundle}
     * @return {@link Bundle}
     */
    public static Bundle request(@NotNull Bundle bundle, IServiceCallback callback) {
        Log.d(TAG, "request");

        long timestamp = SystemClock.elapsedRealtime();

        Bundle output;
        String CMD_ID  = bundle.getString(ABECS.CMD_ID, "UNKNOWN");

        try {
            sRequestSemaphore.acquireUninterruptibly();

            Bundle request  = new Bundle();
            Bundle response = new Bundle();

            int status = 0;

            request.putByteArray("request", PinpadUtility.buildRequestDataPacket(bundle));
            request.putBundle   ("request_bundle", bundle);

            try {
                sExchangeSemaphore.acquireUninterruptibly();

                status = send   (request, callback);

                if (status < 0) {
                    throw new RuntimeException("request::status [" + status + "]");
                }

                status = receive(response, 2000);

                switch (status) {
                    case -2:
                        response.putByteArray("response", new byte[] { 0x04 });
                        break;

                    case  0:
                        throw new TimeoutException();

                    default:
                        if (status > 0) { break; }

                        throw new RuntimeException("request::status [" + status + "]");
                }
            } finally {
                sExchangeSemaphore.release();
            }

            switch (response.getByteArray("response")[0]) {
                case 0x06:
                    /* Nothing to do */
                    break;

                case 0x04:
                    throw new InterruptedException();

                default:
                    String message = String.format("request::response[0] [%02X]", response.getByteArray("response")[0]);

                    throw new RuntimeException(message);
            }

            do {
                // TODO: break loop for non-blocking instructions

                response = new Bundle();

                status   = receive(response, 10000);

                switch (status) {
                    case -2:
                        response.putByteArray("response", new byte[] { 0x04 });
                        break;

                    case  0:
                        /* Nothing to do */
                        break;

                    default:
                        if (status > 0) { break; }

                        throw new RuntimeException("request::status [" + status + "]");
                }
            } while (status <= 0);

            if (response.getByteArray("response")[0] != 0x04) {
                output = response.getBundle("response_bundle");

                if (output == null) {
                    output = PinpadUtility.parseResponseDataPacket(response.getByteArray("response"), status);
                }
            } else {
                throw new InterruptedException();
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            ABECS.STAT RSP_STAT;

            if (exception instanceof InterruptedException) {
                RSP_STAT = ABECS.STAT.ST_CANCEL;
            } else {
                RSP_STAT = ABECS.STAT.ST_INTERR;
            }

            output = new Bundle();

            output.putSerializable(ABECS.RSP_EXCEPTION, exception);
            output.putSerializable(ABECS.RSP_ID, CMD_ID);
            output.putSerializable(ABECS.RSP_STAT, RSP_STAT);
        } finally {
            sRequestSemaphore.release();

            Log.d(TAG, "request::timestamp " + ((!CMD_ID.equals("UNKNOWN")) ? "(" + CMD_ID + ") " : "") + "[" + (SystemClock.elapsedRealtime() - timestamp) + "]");
        }

        return output;
    }

    /**
     * Performs an ABECS PINPAD abort request, interrupting blocking or sequential commands.
     */
    public static void abort() {
        Log.d(TAG, "abort");

        long timestamp = SystemClock.elapsedRealtime();

        try {
            sExchangeSemaphore.acquireUninterruptibly();

            int status = send(new byte[] { 0x18 }, 1, null);

            if (status < 0) {
                throw new RuntimeException("abort::status [" + status + "]");
            }

            Bundle response = new Bundle();

            status = receive(response, 2000);

            switch (status) {
                case -2:
                    response.putByteArray("response", new byte[] { 0x04 });
                    break;

                case  0:
                    throw new TimeoutException();

                default:
                    if (status > 0) { break; }

                    throw new RuntimeException("request::status [" + status + "]");
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));
        } finally {
            sExchangeSemaphore.release();

            Log.d(TAG, "abort::timestamp [" + (SystemClock.elapsedRealtime() - timestamp) + "]");
        }
    }

    /**
     * Starts a new thread and calls {@link Runnable#run()} from given {@link Runnable}.<br>
     * Intended as a helper for UI thread calls.<br>
     * <code>
     *     <pre>
     * PinpadManager.execute(new Runnable() {
     *    {@literal @}Override
     *     public void execute() {
     *         // code you shouldn't run on the main thread goes here
     *     }
     * });
     *     </pre>
     * </code>
     *
     * @param runnable {@link Runnable}
     */
    public static void execute(@NotNull Runnable runnable) {
        Log.d(TAG, "execute");

        ServiceUtility.execute(runnable);
    }

    /**
     * Receives the processing result of a previously sent ABECS PINPAD request.<br>
     * See {@link PinpadManager#send(byte[], int, IServiceCallback)}.
     *
     * @param response {@code byte[]} as specified by ABECS PINPAD protocol
     * @param timeout self-describing (milliseconds)
     * @return {@code int} bigger than zero if the request was processed successfully, less than
     *         zero in the event of a failure and zero if timeout was reached
     */
    public static int receive(byte[] response, long timeout) {
        Log.d(TAG, "receive");

        long timestamp = SystemClock.elapsedRealtime();
        int    result  = 0;

        try {
            if (!sReceiveSemaphore.tryAcquire(timeout, MILLISECONDS)) {
                return result;
            }

            timeout = (SystemClock.elapsedRealtime() - timestamp);
            timeout = (timeout < 0) ? 0 : timeout;

            try {
                Bundle bundle  = new Bundle();

                bundle.putLong("timeout", timeout);

                IBinder binder = ServiceUtility.retrieve(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE);

                result = IPinpadService.Stub.asInterface(binder)
                                .getPinpadManager(null).recv(bundle);

                if (result > 0) {
                    System.arraycopy(bundle.getByteArray("response"), 0, response, 0, result);
                }
            } finally {
                sReceiveSemaphore.release();
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            result = -1;
        } finally {
            Log.h(TAG, response, result);

            Log.d(TAG, "receive::timestamp [" + (SystemClock.elapsedRealtime() - timestamp) + "]");
        }

        return result;
    }

    /**
     * Binds the Pinpad Service.<br>
     * Ensures the binding will be undone in the event of a service disconnection.
     */
    public static void register(@NotNull ServiceUtility.Callback callback) {
        Log.d(TAG, "register");

        ServiceUtility.register(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE, null, callback);
    }

    public static void register(Bundle bundle, @NotNull ServiceUtility.Callback callback) {
        Log.d(TAG, "register");

        ServiceUtility.register(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE, bundle, callback);
    }

    /**
     * Performs an ABECS PINPAD request using the default public data format.<br>
     * Doesn't wait for the processing result.
     *
     * @param request {@code byte[]} as specified by ABECS PINPAD protocol
     * @param length {@code input} length
     * @return {@code int} bigger than zero if the request was sent successfully, less than zero
     *         otherwise
     */
    public static int send(byte[] request, int length, IServiceCallback callback) {
        Log.d(TAG, "send");

        long timestamp = SystemClock.elapsedRealtime();
        int    result  = 0;

        Log.h(TAG, request, length);

        try {
            byte[] courier = new byte[length];

            System.arraycopy(request, 0, courier, 0, length);

            Bundle bundle = new Bundle();

            bundle.putString   ("application_id", Application.getPackageContext().getPackageName());
            bundle.putByteArray("request", courier);

            IBinder binder = ServiceUtility.retrieve(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE);

            result = IPinpadService.Stub.asInterface(binder)
                            .getPinpadManager(null).send(bundle, callback);
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            result = -1;
        } finally {
            Log.d(TAG, "send::timestamp [" + (SystemClock.elapsedRealtime() - timestamp) + "]");
        }

        return result;
    }

    /**
     * Unbinds the Pinpad Service.
     */
    public static void unregister() {
        Log.d(TAG, "unregister");

        ServiceUtility.unregister(PACKAGE_PINPAD_SERVICE, ACTION_PINPAD_SERVICE);
    }
}
