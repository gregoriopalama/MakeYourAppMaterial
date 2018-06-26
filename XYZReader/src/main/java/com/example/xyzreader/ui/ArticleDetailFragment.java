package com.example.xyzreader.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.databinding.FragmentArticleDetailBinding;
import com.example.xyzreader.databinding.ListItemDateBinding;
import com.example.xyzreader.databinding.ListItemTextBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    private FragmentArticleDetailBinding binding;

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentArticleDetailBinding.inflate(inflater, container, false);

        bindViews();
        return binding.getRoot();
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mCursor == null)
            return;

        List<String> items = new ArrayList<>();
        Date publishedDate = parsePublishedDate();
        String date;
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            date = DateUtils.getRelativeTimeSpanString(
                    publishedDate.getTime(),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString()
                    + " by <strong>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</strong>";

        } else {
            // If date is before 1902, just show the string
            date = outputFormat.format(publishedDate) + " by <strong>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</strong>";

        }
        items.add(date);

        for( String s : mCursor.getString(ArticleLoader.Query.BODY).split("(\\r\\n|\\n)")) {
            String trimmed = s.trim();
            if (!TextUtils.isEmpty(trimmed))
                items.add(s);
        }

        Adapter adapter = new Adapter(items);
        binding.list.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        binding.list.setAdapter(adapter);
        binding.list.setHasFixedSize(true);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            if (data != null) {
                data.close();
            }
            return;
        }

        mCursor = data;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int DATE_VIEW_TYPE = 0;
        private static final int TEXT_VIEW_TYPE = 1;
        private List<String> items;

        public Adapter(List<String> items) {
            this.items = items;
        }

        public void setItems(List<String> items) {
            this.items = items;
            this.notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return DATE_VIEW_TYPE;
            return TEXT_VIEW_TYPE;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater =
                    LayoutInflater.from(parent.getContext());
            RecyclerView.ViewHolder viewHolder = null;
            switch (viewType) {
                case DATE_VIEW_TYPE:
                    {
                        ListItemDateBinding binding =
                            ListItemDateBinding.inflate(layoutInflater, parent, false);
                        viewHolder = new DateViewHolder(binding);
                    }
                    break;
                case TEXT_VIEW_TYPE:
                    {
                        ListItemTextBinding binding =
                                ListItemTextBinding.inflate(layoutInflater, parent, false);
                        viewHolder = new TextViewHolder(binding);
                    }
                    break;
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case DATE_VIEW_TYPE:
                    ((DateViewHolder) holder).bind(items.get(position));
                    break;
                case TEXT_VIEW_TYPE:
                    ((TextViewHolder) holder).bind(items.get(position));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    public static class DateViewHolder extends RecyclerView.ViewHolder {
        ListItemDateBinding binding;

        public DateViewHolder(ListItemDateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String date) {
            this.binding.date.setText(Html.fromHtml(date), TextView.BufferType.SPANNABLE);
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        ListItemTextBinding binding;

        public TextViewHolder(ListItemTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String text) {
            this.binding.text.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        }
    }
}
