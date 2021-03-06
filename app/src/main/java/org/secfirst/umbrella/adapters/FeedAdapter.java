package org.secfirst.umbrella.adapters;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.secfirst.umbrella.R;
import org.secfirst.umbrella.models.FeedItem;
import org.secfirst.umbrella.WebViewDialog;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FeedAdapter extends BaseAdapter {

    List<FeedItem> feedItems = new ArrayList<>();
    Context mContext;

    public FeedAdapter(Context context, List<FeedItem> mItems) {
        if (mItems != null) {
            this.feedItems = mItems;
            sortFeedItems(true);
        }
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return feedItems.size();
    }

    @Override
    public Object getItem(int position) {
        return feedItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final FeedItem current = feedItems.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.feed_item, null);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.feed_title);
            holder.body = (TextView) convertView.findViewById(R.id.feed_body);
            holder.date = (TextView) convertView.findViewById(R.id.feed_date);
            holder.card = (CardView) convertView.findViewById(R.id.card_view);
            holder.site = (TextView) convertView.findViewById(R.id.feed_site);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(current.getTitle());
        holder.body.setText(Html.fromHtml(current.getBody()));
        holder.site.setText(convertStringToUrl(current.getUrl()));

        if (current.getDate() > 0) {
            holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(current.getDate() * 1000)));
        }

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Patterns.WEB_URL.matcher(current.getUrl()).matches()) {
                    AppCompatActivity activity = (AppCompatActivity) view.getContext();
                    FragmentManager fragmentManager = activity.getSupportFragmentManager();
                    WebViewDialog webViewDialog = WebViewDialog.newInstance(current.getUrl());
                    webViewDialog.show(fragmentManager, "");
                }
            }
        });
        return convertView;
    }

    private String convertStringToUrl(String urlParam) {
        URL url = null;
        try {
            url = new URL(urlParam);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url != null ? url.getHost() : "";
    }

    public void updateData(ArrayList<FeedItem> updateItems) {
        if (updateItems != null) {
            feedItems = updateItems;
            sortFeedItems(true);
        }
        notifyDataSetChanged();
    }

    private void sortFeedItems(final boolean reverse) {
        Collections.sort(feedItems, new Comparator<FeedItem>() {
            @Override
            public int compare(FeedItem f1, FeedItem f2) {
                if (f1.getDate() > f2.getDate())
                    return (reverse ? -1 : 1);
                if (f1.getDate() < f2.getDate())
                    return (reverse ? 1 : -1);
                return 0;
            }
        });
    }

    private static class ViewHolder {
        public TextView title;
        public TextView body;
        public TextView date;
        public CardView card;
        public TextView site;
    }
}
