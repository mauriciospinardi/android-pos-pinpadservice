package io.cloudwalk.pos.pinpadlibrary.internals.commands;

import android.os.Bundle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.cloudwalk.loglibrary.Log;
import io.cloudwalk.pos.pinpadlibrary.ABECS;
import io.cloudwalk.pos.pinpadlibrary.internals.utilities.PinpadUtility;

public class CEX {
    private static final String
            TAG = CEX.class.getSimpleName();

    private CEX() {
        Log.d(TAG, "CEX");

        /* Nothing to do */
    }

    public static byte[] buildRequestDataPacket(@NotNull Bundle input)
            throws Exception {
        Log.d(TAG, "buildRequestDataPacket");

        List<String> list = new ArrayList<>(0);

        list.add(ABECS.SPE_CEXOPT);
        list.add(ABECS.SPE_TIMEOUT);
        list.add(ABECS.SPE_PANMASK);

        return PinpadUtility.CMD.buildRequestDataPacket(input, list);
    }

    public static byte[] buildResponseDataPacket(@NotNull Bundle input)
            throws Exception {
        Log.d(TAG, "buildResponseDataPacket");

        List<String> list = new ArrayList<>(0);

        list.add(ABECS.PP_EVENT);
        list.add(ABECS.PP_TRK1INC);
        list.add(ABECS.PP_TRK2INC);
        list.add(ABECS.PP_TRK3INC);

        return PinpadUtility.CMD.buildResponseDataPacket(input, list);
    }
}
