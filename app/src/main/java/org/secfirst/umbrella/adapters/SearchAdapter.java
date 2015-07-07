package org.secfirst.umbrella.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.secfirst.umbrella.R;
import org.secfirst.umbrella.models.Category;
import org.secfirst.umbrella.models.DrawerChildItem;
import org.secfirst.umbrella.models.Segment;
import org.secfirst.umbrella.util.Global;
import org.secfirst.umbrella.util.UmbrellaUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    private List<Segment> mSegment = new ArrayList<>();
    private ArrayList<ArrayList<DrawerChildItem>> mSubtitles = new ArrayList<ArrayList<DrawerChildItem>>();
    private ArrayList<Category> mTitles = new ArrayList<Category>();
    private ArrayList<String> mQueries = new ArrayList<String>();
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mSearchText;
        public TextView mTitle;
        public TextView mBody;
        public CardView mCardView;
        public ViewHolder(View v) {
            super(v);
            mSearchText = (TextView) v.findViewById(R.id.search_text);
            mTitle = (TextView) v.findViewById(R.id.search_result_title);
            mBody = (TextView) v.findViewById(R.id.search_result_body);
            mCardView = (CardView) v.findViewById(R.id.card_view);
        }
    }

    public SearchAdapter(Context context, List<Segment> segmentList, String query) {
        mContext = context;
        mSegment = segmentList;
        mSubtitles = UmbrellaUtil.getChildItems(mContext);
        mTitles = UmbrellaUtil.getParentCategories(mContext);
        mQueries.add(query);
    }

    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String queryText = "";
        if (mQueries != null && mQueries.size() > 0) {
            queryText = Jsoup.clean(mQueries.get(0), Whitelist.simpleText());
        }
        holder.mSearchText.setText(Html.fromHtml("result while searching for: <b>" + queryText + "</b>"));
        final Segment current = mSegment.get(position);
        String forTitle = "";
        if (mTitles.size() > current.getCategory())
            forTitle += mTitles.get(current.getCategory()).getCategory();
        if (mSubtitles.size() > current.getCategory() && mSubtitles.get(current.getCategory()).size() >= current.getCategory()) {
            forTitle += ((forTitle.length() > 0) ? " - " : "") + mSubtitles.get(current.getCategory()).get(current.getCategory() - 1).getTitle();
        }
        holder.mTitle.setText(forTitle);
        holder.mBody.setText(Html.fromHtml(searchBody(current.getBody(), queryText)));
        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Category category = null;
                try {
                    category = ((Global) mContext.getApplicationContext()).getDaoCategory().queryForId(String.valueOf(current.getCategory()));
                } catch (SQLException e) {
                    UmbrellaUtil.logIt(mContext, Log.getStackTraceString(e.getCause()));
                }
                if (category == null) {
                    return;
                }
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("umbrella://lesson/" + category.getCategory().replace(' ', '-')));
                mContext.startActivity(i);
            }
        });
    }

    private String searchBody(String body, String query) {
        String lower = body.toLowerCase(Locale.UK);
        int start = lower.indexOf(query.toLowerCase(Locale.UK));
        int offset = 80;
        int from = (start-offset>0) ? start-offset : 0;
        int to = (start+query.length()+offset>body.length()) ? body.length() : start+query.length()+offset;
        String returnBody = "..."+body.substring(from, to)+"...";

        returnBody = returnBody.replaceAll("(?i)"+query, "<b>"+query+"</b>");
        return returnBody;
    }

    @Override
    public int getItemCount() {
        return mSegment.size();
    }
}