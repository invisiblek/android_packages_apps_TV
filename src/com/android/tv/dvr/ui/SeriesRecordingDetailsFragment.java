/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tv.dvr.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;

import com.android.tv.R;
import com.android.tv.TvApplication;
import com.android.tv.data.BaseProgram;
import com.android.tv.data.Channel;
import com.android.tv.dvr.DvrDataManager;
import com.android.tv.dvr.DvrUiHelper;
import com.android.tv.dvr.RecordedProgram;
import com.android.tv.dvr.SeriesRecording;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link DetailsFragment} for series recording in DVR.
 */
public class SeriesRecordingDetailsFragment extends DvrDetailsFragment implements
        DvrDataManager.SeriesRecordingListener, DvrDataManager.RecordedProgramListener {
    private static final int ACTION_SERIES_SCHEDULES = 1;
    private static final int ACTION_DELETE = 2;

    private DvrDataManager mDvrDataManager;

    private SeriesRecording mSeries;
    // NOTICE: mRecordedPrograms should only be used in creating details fragments.
    // After fragments are created, it should be cleared to save resources.
    private List<RecordedProgram> mRecordedPrograms;
    private DetailsContent mDetailsContent;
    private int mSeasonRowCount;
    private SparseArrayObjectAdapter mActionsAdapter;
    private Action mDeleteAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDvrDataManager = TvApplication.getSingletons(getActivity()).getDvrDataManager();
        super.onCreate(savedInstanceState);
        setDetailsOverviewRow(mDetailsContent);
        setupRecordedProgramsRow();
        mDvrDataManager.addSeriesRecordingListener(this);
        mDvrDataManager.addRecordedProgramListener(this);
        mRecordedPrograms = null;
    }

    @Override
    protected boolean onLoadRecordingDetails(Bundle args) {
        long recordId = args.getLong(DvrDetailsActivity.RECORDING_ID);
        mSeries = TvApplication.getSingletons(getActivity()).getDvrDataManager()
                .getSeriesRecording(recordId);
        if (mSeries == null) {
            return false;
        }
        mRecordedPrograms = mDvrDataManager.getRecordedPrograms(mSeries.getId());
        Collections.sort(mRecordedPrograms, RecordedProgram.SEASON_REVERSED_EPISODE_COMPARATOR);
        mDetailsContent = createDetailsContent();
        return true;
    }

    @Override
    protected PresenterSelector onCreatePresenterSelector(
            DetailsOverviewRowPresenter rowPresenter) {
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        return presenterSelector;
    }

    private DetailsContent createDetailsContent() {
        Channel channel = TvApplication.getSingletons(getContext()).getChannelDataManager()
                .getChannel(mSeries.getChannelId());
        String description = TextUtils.isEmpty(mSeries.getLongDescription())
                ? mSeries.getDescription() : mSeries.getLongDescription();
        return new DetailsContent.Builder()
                .setTitle(mSeries.getTitle())
                .setDescription(description)
                .setImageUris(mSeries.getPosterUri(), mSeries.getPhotoUri(), channel)
                .build();
    }

    @Override
    protected SparseArrayObjectAdapter onCreateActionsAdapter() {
        mActionsAdapter = new SparseArrayObjectAdapter(new ActionPresenterSelector());
        Resources res = getResources();
        mActionsAdapter.set(ACTION_SERIES_SCHEDULES, new Action(ACTION_SERIES_SCHEDULES,
                getString(R.string.dvr_detail_view_schedule), null,
                res.getDrawable(R.drawable.ic_schedule_32dp, null)));
        mDeleteAction = new Action(ACTION_DELETE,
                getString(R.string.dvr_detail_series_delete), null,
                res.getDrawable(R.drawable.ic_delete_32dp, null));
        if (!mRecordedPrograms.isEmpty()) {
            mActionsAdapter.set(ACTION_DELETE, mDeleteAction);
        }
        return mActionsAdapter;
    }

    private void setupRecordedProgramsRow() {
        for (RecordedProgram program : mRecordedPrograms) {
            addProgram(program);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDvrDataManager.removeSeriesRecordingListener(this);
        mDvrDataManager.removeRecordedProgramListener(this);
        if (mSeries.getState() == SeriesRecording.STATE_SERIES_CANCELED
                && mDvrDataManager.getRecordedPrograms(mSeries.getId()).isEmpty()) {
            TvApplication.getSingletons(getActivity()).getDvrManager()
                    .removeSeriesRecording(mSeries.getId());
        }
    }

    @Override
    protected OnActionClickedListener onCreateOnActionClickedListener() {
        return new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_SERIES_SCHEDULES) {
                    DvrUiHelper.startSchedulesActivityForSeries(getContext(), mSeries);
                } else if (action.getId() == ACTION_DELETE) {
                    DvrUiHelper.startSeriesDeletionActivity(getContext(), mSeries.getId());
                }
            }
        };
    }

    @Override
    public void onSeriesRecordingAdded(SeriesRecording... seriesRecordings) { }

    @Override
    public void onSeriesRecordingChanged(SeriesRecording... seriesRecordings) {
        for (SeriesRecording series : seriesRecordings) {
            if (mSeries.getId() == series.getId()) {
                mSeries = series;
                // TODO: change action label.
            }
        }
    }

    @Override
    public void onSeriesRecordingRemoved(SeriesRecording... seriesRecordings) { }

    @Override
    public void onRecordedProgramAdded(RecordedProgram recordedProgram) {
        if (TextUtils.equals(recordedProgram.getSeriesId(), mSeries.getSeriesId())) {
            addProgram(recordedProgram);
            if (mActionsAdapter.lookup(ACTION_DELETE) == null) {
                mActionsAdapter.set(ACTION_DELETE, mDeleteAction);
            }
        }
    }

    @Override
    public void onRecordedProgramChanged(RecordedProgram recordedProgram) {
        // Do nothing
    }

    @Override
    public void onRecordedProgramRemoved(RecordedProgram recordedProgram) {
        if (TextUtils.equals(recordedProgram.getSeriesId(), mSeries.getSeriesId())) {
            ListRow row = getSeasonRow(recordedProgram.getSeasonNumber(), false);
            if (row != null) {
                SeasonRowAdapter adapter = (SeasonRowAdapter) row.getAdapter();
                adapter.remove(recordedProgram);
                if (adapter.isEmpty()) {
                    getRowsAdapter().remove(row);
                    if (getRowsAdapter().size() == 1) {
                        // No season rows left. Only DetailsOverviewRow
                        mActionsAdapter.clear(ACTION_DELETE);
                    }
                }
            }
        }
    }

    private void addProgram(RecordedProgram program) {
        String programSeasonNumber =
                TextUtils.isEmpty(program.getSeasonNumber()) ? "" : program.getSeasonNumber();
        getOrCreateSeasonRowAdapter(programSeasonNumber).add(program);
    }

    private SeasonRowAdapter getOrCreateSeasonRowAdapter(String seasonNumber) {
        ListRow row = getSeasonRow(seasonNumber, true);
        return (SeasonRowAdapter) row.getAdapter();
    }

    private ListRow getSeasonRow(String seasonNumber, boolean createNewRow) {
        seasonNumber = TextUtils.isEmpty(seasonNumber) ? "" : seasonNumber;
        ArrayObjectAdapter rowsAdaptor = getRowsAdapter();
        for (int i = rowsAdaptor.size() - 1; i >= 0; i--) {
            Object row = rowsAdaptor.get(i);
            if (row instanceof ListRow) {
                int compareResult = RecordedProgram.numberCompare(seasonNumber,
                        ((SeasonRowAdapter) ((ListRow) row).getAdapter()).mSeasonNumber);
                if (compareResult == 0) {
                    return (ListRow) row;
                } else if (compareResult < 0) {
                    return createNewRow ? createNewSeasonRow(seasonNumber, i + 1) : null;
                }
            }
        }
        return createNewRow ? createNewSeasonRow(seasonNumber, rowsAdaptor.size()) : null;
    }

    private ListRow createNewSeasonRow(String seasonNumber, int position) {
        String seasonTitle = seasonNumber.isEmpty() ? mSeries.getTitle()
                : getString(R.string.dvr_detail_series_season_title, seasonNumber);
        HeaderItem header = new HeaderItem(mSeasonRowCount++, seasonTitle);
        ClassPresenterSelector selector = new ClassPresenterSelector();
        selector.addClassPresenter(RecordedProgram.class,
                new RecordedProgramPresenter(getContext(), true));
        ListRow row = new ListRow(header, new SeasonRowAdapter(selector,
                new Comparator<RecordedProgram>() {
                    @Override
                    public int compare(RecordedProgram lhs, RecordedProgram rhs) {
                        return BaseProgram.EPISODE_COMPARATOR.compare(lhs, rhs);
                    }
                }, seasonNumber));
        getRowsAdapter().add(position, row);
        return row;
    }

    private class SeasonRowAdapter extends SortedArrayAdapter<RecordedProgram> {
        private String mSeasonNumber;

        SeasonRowAdapter(PresenterSelector selector, Comparator<RecordedProgram> comparator,
                String seasonNumber) {
            super(selector, comparator);
            mSeasonNumber = seasonNumber;
        }

        @Override
        public long getId(RecordedProgram program) {
            return program.getId();
        }
    }
}