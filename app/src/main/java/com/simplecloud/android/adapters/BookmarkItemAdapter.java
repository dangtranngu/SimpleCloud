package com.simplecloud.android.adapters;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplecloud.android.R;
import com.simplecloud.android.common.utils.DateUtils;
import com.simplecloud.android.models.files.FileSystemObject;

public class BookmarkItemAdapter extends ArrayAdapter<FileSystemObject> {

    private View mView;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<FileSystemObject> mList;
    
    
    public BookmarkItemAdapter(Context mContext, List<FileSystemObject> mList) {
        super(mContext, R.layout.navigation_item,mList);
        this.mContext = mContext;
        this.mList = mList;
        mInflater = ((Activity) mContext).getLayoutInflater();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parentUId) {
        ViewHolder holder = null;
        mView = convertView;
        if (mView == null) {
            mView = mInflater.inflate(R.layout.bookmark_item, parentUId, false);
            holder = new ViewHolder();
            
            holder.resName = (TextView)mView.findViewById(R.id.bookmark_fName);
            holder.resIcon = (ImageView)mView.findViewById(R.id.bookmark_fIcon);
            holder.resPath = (TextView)mView.findViewById(R.id.bookmark_fPath);
            mView.setTag(holder);
            
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        
        FileSystemObject f = mList.get(position);
        holder.resName.setText(f.getName());
        holder.resIcon.setImageDrawable(getIcon(f));
        
        long lastModified = 0;
        if (f.getAccId() == 0) {
            lastModified = new File(f.getUId()).lastModified();
        } else {
            lastModified = f.getLastModifiedTime();
        }
        holder.resPath.setText("Last modified: " + DateUtils.getDateCurrentTimeZone(lastModified));


        return mView;
    }
    

    private Drawable getIcon(FileSystemObject f) {
        return mContext.getResources().getDrawable(f.getIconId());
    }
    public static class ViewHolder {
        public TextView resName;
        public TextView resPath;
        public ImageView resIcon;
    }

}
