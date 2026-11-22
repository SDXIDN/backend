package com.course.app.scheduling;

import org.quartz.JobDataMap;

/**
 * Читання параметрів з JobDataMap з явною помилкою замість тихого значення за замовчуванням:
 * опечатка в ключі або відсутній ключ -> IllegalArgumentException з зрозумілим повідомленням.
 */
public final class JobParams {

    private JobParams() {
    }

    public static int requireInt(JobDataMap data, String key) {
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException("Missing JobDataMap key '" + key + "'");
        }
        try {
            return data.getInt(key);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException("JobDataMap key '" + key + "' is not an integer", ex);
        }
    }
}
