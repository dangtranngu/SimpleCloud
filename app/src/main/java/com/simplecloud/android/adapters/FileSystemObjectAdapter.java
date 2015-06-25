package com.simplecloud.android.adapters;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.simplecloud.android.R;
import com.simplecloud.android.background.CopyFilesAsync.OnListViewProgressBarUpdateListener;
import com.simplecloud.android.background.CopyManager;
import com.simplecloud.android.background.CopyManager.CopyState;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileSystemObject;

public class FileSystemObjectAdapter extends ArrayAdapter<FileSystemObject> 
        implements OnListViewProgressBarUpdateListener {

    private View mView;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<FileSystemObject> mList;
    
    private SparseBooleanArray mSelection = new SparseBooleanArray();
    
    private static ProgressBar mProgressBar;
    private static int accumulatedProgress; //DONT CHANGE
    private int mCopiedFilePosition;
    
    public FileSystemObjectAdapter(Context mContext, List<FileSystemObject> mList) {
        super(mContext, R.layout.navigation_item, mList);
        this.mContext = mContext;
        this.mList = mList;
        mInflater = ((Activity) mContext).getLayoutInflater();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parentUId) {
        ViewHolder holder = null;
        mView = convertView;
        if (mView == null) {
            mView = mInflater.inflate(R.layout.navigation_item, parentUId, false);
            holder = new ViewHolder();
            
            holder.resName = (TextView)mView.findViewById(R.id.explorer_fName);
            holder.resIcon = (ImageView)mView.findViewById(R.id.explorer_fIcon);
            holder.resCheckbox = (CheckBox) mView.findViewById(R.id.explorer_checkbox);
            holder.resBar = (ProgressBar) mView.findViewById(R.id.explorer_progressbar);
            holder.resBarMsg = (TextView)mView.findViewById(R.id.explorer_progressbar_message);
            mView.setTag(holder);
            
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        
        FileSystemObject f = mList.get(position);

        if (mSelection.size() > 0) {
            holder.resCheckbox.setVisibility(View.VISIBLE);
            holder.resCheckbox.setChecked(mSelection.get(position));
        } else {
            holder.resCheckbox.setVisibility(View.INVISIBLE);
        }
        
        holder.resName.setText(f.getName());
        holder.resIcon.setImageDrawable(getIcon(f));

        CopyManager copySmgr = CopyManager.getCopyManager();
        
        if (copySmgr.getState() == CopyState.IN_PROGRESS
                && f.getUId().equals(CopyManager.FILE_IN_PROGRESS_UID)) {
            
            holder.resBar.setVisibility(View.VISIBLE);
            holder.resBarMsg.setVisibility(View.VISIBLE);
            holder.resCheckbox.setVisibility(View.INVISIBLE);
            
            if (mProgressBar != null && mProgressBar != holder.resBar) {
                long size = f.getSize();
                holder.resBarMsg.setText(FileUtils.readableFileSize(size));

                if (!copySmgr.isIndeterminate()) {
                    holder.resBar.setMax(mProgressBar.getMax());
                    holder.resBar.setProgress(accumulatedProgress);
                } else 
                    holder.resBar.setIndeterminate(true);
                
                mProgressBar = holder.resBar;
                mCopiedFilePosition = position;
                
            }
            
        } else if (holder.resBar.getVisibility() == View.VISIBLE) {
            holder.resBar.setVisibility(View.INVISIBLE);
            holder.resBarMsg.setVisibility(View.INVISIBLE);
        }

        return mView;
    }
    
    public void switchSelection(int position) {
        if (!mSelection.get(position))
            mSelection.put(position, true);
        else 
            mSelection.delete(position);
        
        notifyDataSetChanged();
    }

    public boolean isPositionChecked(int position) {
        return mSelection.get(position);
    }

    public Set<Integer> getCurrentCheckedPosition() {
        Set<Integer> positions = new HashSet<Integer>();
        for (int i = 0; i < mSelection.size(); i++) {
            positions.add(mSelection.keyAt(i));
        }
        return positions;
    }


    public void clearSelection() {
        mSelection = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public int getCopiedFilePosition() {
        return mCopiedFilePosition;
    }
    
    public static class ViewHolder {
        public TextView resName;
        public ImageView resIcon;
        public CheckBox resCheckbox;
        public ProgressBar resBar;
        public TextView resBarMsg;
    }

    private Drawable getIcon(FileSystemObject obj) {
        return mContext.getResources().getDrawable(obj.getIconId());
    }

    @Override
    public void onPrepareProgressBar(long max) {
        mProgressBar = new ProgressBar(mContext);

        if (max > Integer.MAX_VALUE)
            max = (int) max / 100;
        mProgressBar.setMax((int) max);
        accumulatedProgress = 0;
    }

    @Override
    public void onIncrementProgress(float rate) {
        int increment = (int) (rate * mProgressBar.getMax());
        mProgressBar.incrementProgressBy(increment);
        accumulatedProgress += increment;
    }

    @Override
    public void onFinishedProgress() {
        // No implementation
    }

    
    /**
     * Sort the file explorer in descending order by:
     * <p><ul>
     * <li>Directories on top of files
     * <li>Name
     * </ul><p>
     */
    public static class FileSystemObjectComparator implements Comparator<FileSystemObject> {

        @Override
        public int compare(FileSystemObject f1, FileSystemObject f2) {
            if (f1 instanceof DirectoryObject) {
                if (f2 instanceof DirectoryObject) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                } else {
                    return -1;
                }
                
            } else {
                if (f2 instanceof DirectoryObject) {
                    return 1;
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            }
        }
    }

}
