package com.example.poc2104301453;

import com.example.poc2104301453.IServiceCallback;

/**
 * ABECS Service Callbacks Interface.<br>
 * Provides a interface set for status callbacks alongside ABECS classical processing callbacks.
 */
interface IServiceCallback {
    /**
     * Status callback.<br>
     * As the name states, its called upon a processing failure.<br>
     * {@code output} will return the "status" key. The "exception" key may also be present,
     * providing further details on the failure.
     *
     * @param Bundle {@link Bundle}
     */
    void onFailure(inout Bundle output);

    /**
     * Status callback.<br>
     * As the name states, its called when the processing of a request finishes sucessfully.<br>
     * {@code output} should return the "status" key at the bare minimum. Other keys may be
     * present, conditionally to the request that was just processed. See the specification v2.12
     * from ABECS for further details.
     *
     * @param Bundle {@link Bundle}
     */
    void onSuccess(inout Bundle output);
}
