package com.simplecloud.android.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simplecloud.android.R;
import com.simplecloud.android.activities.NavigationActivity;
import com.simplecloud.android.models.drawers.AccountDrawerItem;
import com.simplecloud.android.models.drawers.DrawerItem;
import com.simplecloud.android.models.drawers.HomeDrawerItem;

public class NavigationDrawerItemAdapter extends ArrayAdapter<DrawerItem>  {

    private final Context mContext;

    private final LayoutInflater mInflater;
    private final List<DrawerItem> mDrawerItems;
    private OnDrawerBookmarkIconCLickListener mBookmarkListener;

    public NavigationDrawerItemAdapter(Context context, List<DrawerItem> items) {
        super(context, R.layout.drawer_list_item, items);
        this.mContext = context;
        this.mDrawerItems = items;
        mInflater = ((Activity) mContext).getLayoutInflater();
        mBookmarkListener = (NavigationActivity) mContext;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DrawerItemHolder holder = null;
        View view = convertView;
        
        if (view == null) {
            view = mInflater.inflate(R.layout.drawer_list_item, parent, false);
            holder = new DrawerItemHolder();

            holder.title = (TextView) view.findViewById(R.id.drawer_title);

            holder.itemLayout = (LinearLayout) view.findViewById(R.id.drawer_item_layout);
            holder.itemName = (TextView) view.findViewById(R.id.drawer_name);
            holder.icon = (ImageView) view.findViewById(R.id.drawer_icon);
            holder.bookmark = (ImageButton) view.findViewById(R.id.drawer_bookmark);
            
            view.setTag(holder);

        } else {
            holder = (DrawerItemHolder) view.getTag();
        }

        final DrawerItem item = mDrawerItems.get(position);

        if (item.getTitle() == null) {
            holder.title.setVisibility(LinearLayout.INVISIBLE);
            holder.itemLayout.setVisibility(LinearLayout.VISIBLE);
            holder.itemName.setText(item.getName());
            holder.icon.setImageDrawable(getIcon(item));
            
            if (item instanceof AccountDrawerItem || item instanceof HomeDrawerItem) {
                holder.bookmark.setVisibility(View.VISIBLE);
                holder.bookmark.setOnClickListener(new OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        mBookmarkListener.onBookmarkIconClick(item);
                    }
                });
            } else {
                holder.bookmark.setVisibility(View.INVISIBLE);
            }
            
            if (item.getId() == NavigationActivity.getAccountId()) {
                holder.itemName.setTypeface(null, Typeface.BOLD_ITALIC);
                holder.itemName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            } else {
                holder.itemName.setTypeface(Typeface.DEFAULT);
                holder.itemName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        } else {
            holder.itemLayout.setVisibility(LinearLayout.INVISIBLE);
            holder.title.setText(item.getTitle());
            view.setEnabled(false);
            view.setOnClickListener(null);
        }

        return view;
    }

    
    private Drawable getIcon(DrawerItem obj) {
        return mContext.getResources().getDrawable(obj.getIcon());
    }
    
    public interface OnDrawerBookmarkIconCLickListener {
        void onBookmarkIconClick(DrawerItem item);
    }

    private static class DrawerItemHolder {
        TextView itemName, title;
        ImageView icon;
        ImageButton bookmark;
        LinearLayout itemLayout;
    }
}