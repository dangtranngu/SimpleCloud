package com.simplecloud.android.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.simplecloud.android.R;
import com.simplecloud.android.adapters.BookmarkItemAdapter;
import com.simplecloud.android.adapters.FileSystemObjectAdapter.FileSystemObjectComparator;
import com.simplecloud.android.common.ui.SwipeDismissListViewTouchListener;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.SharedPrefUtils;
import com.simplecloud.android.database.FileSystemObjectDAO;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.LocalDirectory;


public class BookmarkFragment extends ListFragment {

    protected List<FileSystemObject> mBookmarkList = new ArrayList<FileSystemObject>();
    protected BookmarkItemAdapter mAdapter;
    private OnBookmarkItemClickListener mBookmarkListener;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mBookmarkListener = (OnBookmarkItemClickListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new BookmarkItemAdapter(getActivity(), mBookmarkList);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
    
        View rootView = inflater.inflate(R.layout.bookmark_fragment, container, false);
        return rootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initBookmarkListView();
        setListAdapter(mAdapter);
    }

    /**
     * Populate the listview with files and/or subdirectories of the current directory
     */
    private void initBookmarkListView() {
        final ListView bookmarkListView = getListView();
        bookmarkListView.setAdapter(mAdapter);
        bookmarkListView.setTextFilterEnabled(true);
        
        bookmarkListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentUId, View view, int position, long id) {
                FileSystemObject f = (FileSystemObject)bookmarkListView.getAdapter().getItem(position);
                mBookmarkListener.onBookmarkItemClick(f);
            }
        });

        SwipeDismissListViewTouchListener touchListener =
            new SwipeDismissListViewTouchListener(
                bookmarkListView,
                new com.simplecloud.android.common.ui.SwipeDismissListViewTouchListener.DismissCallbacks() {
                    public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            FileSystemObject f = mAdapter.getItem(position);
                            mAdapter.remove(f);
                            FileSystemObjectDAO mFileDAO = new FileSystemObjectDAO(getActivity());
                            if (f.getAccId() == 0)
                                mFileDAO.deleteLocalBookmark(f.getUId());
                            else 
                                mFileDAO.setBookmark(f.getAccId(), f.getUId(), false);
                        }
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public boolean canDismiss(int position) {
                        return true;
                    }
                });
        bookmarkListView.setOnTouchListener(touchListener);
        bookmarkListView.setOnScrollListener(touchListener.makeScrollListener());
        registerForContextMenu(bookmarkListView);
    }
    
    public void populateBookmarkList(Context context, int accId) {
        FileSystemObjectDAO mFileDAO = new FileSystemObjectDAO(context);
        mBookmarkList.clear();
        mBookmarkList.addAll(mFileDAO.getBookmarks(accId));
        
        if (accId == 0) {
            Iterator<FileSystemObject> iter = mBookmarkList.iterator();
            while (iter.hasNext()) {
                String path  = iter.next().getUId();
                if (!new File(path).exists()) {
                    iter.remove();
                    mFileDAO.deleteLocalBookmark(path);
                }
            }
        }
        
        Collections.sort(mBookmarkList, new FileSystemObjectComparator());
        
        if (accId == 0) {
            DirectoryObject home = new LocalDirectory(FileUtils.getSDCardDirectory());
            home.setName("SD Card");
            home.setIconId(R.drawable.directory_home);
            DirectoryObject download = new LocalDirectory(
                new File(SharedPrefUtils.getDownloadDirectory(context)));
            download.setName("Download");
            download.setIconId(R.drawable.directory_download);
            
            mBookmarkList.add(0, home);
            mBookmarkList.add(1, download);
        }
        
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }
    
    public interface OnBookmarkItemClickListener {
        void onBookmarkItemClick(FileSystemObject file);
    }
}
