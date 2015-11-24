
package info.guardianproject.utils;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

public class FolderIterator {
    public static File pathToSDCard;

    public FolderIterator() {
        pathToSDCard = Environment.getExternalStorageDirectory();
    }

    public static ArrayList<File> getFoldersOnSDCard() {
        File[] folder = pathToSDCard.listFiles();
        ArrayList<File> folders = new ArrayList<File>();

        folders.add(pathToSDCard);

        for (File f : folder) {
            if (f.isDirectory() && f.canWrite()) {
                folders.add(f);
            }
        }

        return folders;
    }
}
