package io.cloudwalk.pos.pinpadservice.managers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.os.Bundle;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Semaphore;

import io.cloudwalk.loglibrary.Log;
import io.cloudwalk.pos.pinpadlibrary.ABECS;
import io.cloudwalk.pos.pinpadlibrary.IPinpadManager;
import io.cloudwalk.pos.pinpadlibrary.IServiceCallback;
import io.cloudwalk.pos.pinpadlibrary.internals.utilities.PinpadUtility;
import io.cloudwalk.pos.pinpadservice.utilities.CallbackUtility;
import io.cloudwalk.pos.pinpadservice.utilities.RouterUtility;

public class PinpadManager extends IPinpadManager.Stub {
    private static final String
            TAG = PinpadManager.class.getSimpleName();

    private static final PinpadManager
            sPinpadManager = new PinpadManager();

    private static final Semaphore
            sRecvSemaphore = new Semaphore(1, true);

    private static final Semaphore
            sSendSemaphore = new Semaphore(1, true);

    private PinpadManager() {
        Log.d(TAG, "PinpadManager");
    }

    public static PinpadManager getInstance() {
        Log.d(TAG, "getInstance");

        return sPinpadManager;
    }

    @Override
    public int recv(@NotNull Bundle bundle) {
        Log.d(TAG, "recv");

        long timeout   = bundle.getLong("timeout", 0);
             timeout   = (timeout < 0) ? 0 : timeout;

        long timestamp = SystemClock.elapsedRealtime() + timeout;
        int  result    = 0;

        try {
            if (!sRecvSemaphore.tryAcquire(timeout, MILLISECONDS)) {
                return result;
            }

            try {
                while (SystemClock.elapsedRealtime() < timestamp) {
                    Bundle response = RouterUtility.sResponseQueue.poll();

                    if (response != null) {
                        bundle.putAll(response);

                        return bundle.getByteArray("response").length;
                    }
                }
            } finally {
                sRecvSemaphore.release();
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            result = -1;
        }

        return result;
    }

    @Override
    public int send(@NotNull Bundle bundle, IServiceCallback callback) {
        Log.d(TAG, "send");

        sSendSemaphore.acquireUninterruptibly();

        int result = 0;

        String applicationId = bundle.getString   ("application_id");
        byte[] request       = bundle.getByteArray("request");
        Bundle requestBundle = bundle.getBundle   ("request_bundle");

        try {
            switch (request[0]) {
                case 0x18:
                    if (request.length == 1) {
                        RouterUtility.abort();
                        break;
                    }
                    /* no break; */

                default:
                    if (requestBundle == null) {
                        requestBundle = PinpadUtility.parseRequestDataPacket(request, request.length);

                        bundle.putBundle("request_bundle", requestBundle);
                    }

                    CallbackUtility.setServiceCallback(callback);

                    switch (requestBundle.getString(ABECS.CMD_ID, "UNKNOWN")) {
                        case ABECS.OPN: case ABECS.GIX: case ABECS.CLX:
                        case ABECS.CEX: case ABECS.CHP: case ABECS.EBX: case ABECS.GCD:
                        case ABECS.GTK: case ABECS.MNU: case ABECS.GPN: case ABECS.RMC:
                        case ABECS.TLI: case ABECS.TLR: case ABECS.TLE:
                        case ABECS.GCX: case ABECS.GED: case ABECS.GOX: case ABECS.FCX:
                            RouterUtility.process(bundle);
                            break;

                        default:
                            throw new IllegalArgumentException();
                    }
            }
        } catch (Exception exception) {
            Log.e(TAG, Log.getStackTraceString(exception));

            Bundle response = new Bundle();

            response.putString   ("application_id", applicationId);
            response.putByteArray("response",       new byte[] { 0x15 });

            RouterUtility.process(response);

            result = -1;
        }

        sSendSemaphore.release();

        return result;
    }
}
