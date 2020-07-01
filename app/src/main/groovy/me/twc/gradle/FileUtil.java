package me.twc.gradle;

import java.io.File;
import java.io.FileFilter;

/**
 * @author 唐万超
 */
public abstract class FileUtil {


    public static String getLatestApkName(String dirPath){
        return getLatestApkName(new File(dirPath));
    }


    /**
     *
     * 获取指定文件夹下最新 apk 文件名
     *
     * @param dirFile 文件夹
     * @return 最新 apk 文件名,或者 null
     */
    public static String getLatestApkName(File dirFile){
        if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()){
            return null;
        }

        final File[] apks = dirFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isDirectory() && pathname.getName().endsWith(".apk");
            }
        });
        if(apks == null){
            return null;
        }

        File latestApk = null;
        for (File apk : apks) {
            if (latestApk == null){
                latestApk = apk;
                continue;
            }
            if (apk.lastModified() > latestApk.lastModified()){
                latestApk = apk;
            }
        }
        return latestApk != null ? latestApk.getName() : null;
    }
}
