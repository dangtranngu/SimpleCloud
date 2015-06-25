package com.simplecloud.android.models.files;

import org.apache.commons.io.FilenameUtils;

import android.util.SparseArray;

import com.simplecloud.android.R;

public class FileObject extends FileSystemObject {
    
    public FileObject() {

    }

    public FileObject(int accId, String uid, String parentUId, String name) {
        super(accId, uid, parentUId, name, 0);
    }
    
    public FileObject(FileObject file) {
        super(file);
    }

    private int getIconIdByExtension() {
        String extension = FilenameUtils.getExtension(getName()).toUpperCase();
        SparseArray<String[]> icons = new  SparseArray<String[]>();
        
        icons.put(R.drawable.filetype_image, 
            new String[]{"JPG","JPEG","TIFF","PNG","GIF","BMP","ICO"});
        
        icons.put(R.drawable.filetype_video, 
            new String[]{"3GP","AVI","FLV","M4V","MOV","MP4","MPG", "SWF","F4V","MKV","MPEG","OGV","3GPP","WMV"});
        
        icons.put(R.drawable.filetype_music, 
            new String[]{"WAV","MP3","FLAC","AIFF","ALAC","M4A","OGG","WMA","M4P"});
        
        icons.put(R.drawable.filetype_text, 
            new String[]{"TXT","SH","BAT","PHP","JAVA"});
        
        icons.put(R.drawable.filetype_zip, 
            new String[]{"GZ","ZIP","RAR","APK"});
        
        icons.put(R.drawable.filetype_doc, 
            new String[]{"DOC","DOCX","ODT", "RTF"});
        
        icons.put(R.drawable.filetype_spreadsheet, 
            new String[]{"XLS","XLSX","ODS"});
        
        icons.put(R.drawable.filetype_slideshow, 
            new String[]{"PPT","PPTX","ODP"});
        
        icons.put(R.drawable.filetype_dev, 
            new String[]{"XML","HTML","XSLT", "JAVA", "PHP"});
        
        icons.put(R.drawable.filetype_pdf, 
            new String[]{"PDF"});

        for (int i = 0; i < icons.size(); i++) {
            for (String ex : icons.valueAt(i)) {
                if (ex.equals(extension)) 
                    return icons.keyAt(i);
            }
        }
        
        return R.drawable.filetype_unknown;
    }
    
    @Override
    public int getIconId() {
        if (iconId == 0)
            iconId = getIconIdByExtension();
        return iconId;
    }

}
