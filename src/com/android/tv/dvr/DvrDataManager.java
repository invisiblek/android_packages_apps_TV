/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tv.dvr;

import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.Range;

import java.util.List;

/**
 * Read only data manager.
 */
@MainThread
public interface DvrDataManager {
    long NEXT_START_TIME_NOT_FOUND = -1;

    boolean isInitialized();

    /**
     * Returns recordings.
     */
    List<Recording> getRecordings();

    /**
     * Returns past recordings.
     */
    List<Recording> getFinishedRecordings();

    /**
     * Returns started recordings.
     */
    List<Recording> getStartedRecordings();

    /**
     * Returns scheduled recordings
     */
    List<Recording> getScheduledRecordings();

    /**
     * Returns season recordings.
     */
    List<SeasonRecording> getSeasonRecordings();

    /**
     * Returns the next start time after {@code time} or {@link #NEXT_START_TIME_NOT_FOUND}
     * if none is found.
     *
     * @param time time milliseconds
     */
    long getNextScheduledStartTimeAfter(long time);

    /**
     * Returns a list of all Recordings with a overlap with the given time period inclusive.
     *
     * <p> A recording overlaps with a period when
     * {@code recording.getStartTime() <= period.getUpper() &&
     * recording.getEndTime() >= period.getLower()}.
     *
     * @param period a time period in milliseconds.
     */
    List<Recording> getRecordingsThatOverlapWith(Range<Long> period);

    /**
     * Add a {@link Listener}.
     */
    void addListener(Listener listener);

    /**
     * Remove a {@link Listener}.
     */
    void removeListener(Listener listener);

    /**
     * Returns the recording with the given recordingId or null if is not found
     */
    @Nullable
    Recording getRecording(long recordingId);

    interface Listener {
        void onRecordingAdded(Recording recording);
        void onRecordingRemoved(Recording recording);
        void onRecordingStatusChanged(Recording recording);
    }
}
