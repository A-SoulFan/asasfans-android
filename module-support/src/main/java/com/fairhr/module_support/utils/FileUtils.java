package com.fairhr.module_support.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.fairhr.module_support.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;

public class FileUtils {

    public static String getFromAssets(Context context, String fileName, String separator){
        String result = "";
        BufferedReader br = null;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            br = new BufferedReader(
                    new InputStreamReader(
                            context.getResources().getAssets().open(fileName)
                    )
            );
            String line = null;
            while (br.readLine() != null){
                stringBuilder.append(line);
                if (!TextUtils.isEmpty(separator)) {
                    stringBuilder.append(separator);
                }
            }

            result = stringBuilder.toString();
            if (!TextUtils.isEmpty(separator) && result.length() > separator.length()) {
                result = result.substring(0, result.length() - separator.length());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                    br = null;
                } catch (Exception e1) {
                    //do nothing
                }
            }
        }
        return result;
    }

    public static File getUriForFile(Context context, Uri uri){
        String result;
        Cursor cursor = context.getContentResolver().query(
                uri, new String[]{MediaStore.Images.ImageColumns.DATA},
                null, null, null
        );
        if (cursor == null)
            result = uri.getPath().toString();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(index);
            cursor.close();
        }
        return new File(result);
    }

    public static Uri getFileUri(Context context, File file){
        return FileProvider.getUriForFile(context,
                context.getString(R.string.support_file_provider_authorities),
                file);
    }

    public static void saveFile2Gallery(Context context,File file) {
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(),
                    file.getName(),
                    null);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = getFileUri(context, file);
            intent.setData(uri);
            context.sendBroadcast(intent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @description ?????????????????????
     * @date        2021/5/10
     * @author      ysw
     * @param
     * @return
     */
    public static File saveBitmap2Gallery(Context context, Bitmap bmp,String title){
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bmp, title, null);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.parse(path);
        intent.setData(uri);
        context.sendBroadcast(intent);
        return getUriForFile(context, uri);
    }

    public static String saveBitmap2File(Bitmap bitmap, String filePath){
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        if(parentFile == null || !parentFile.exists()){
            parentFile.mkdirs();
        }

        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {

        }
        return filePath;
    }


    /**
     * ???????????????????????????
     *
     * @param folderPath ?????????path
     * @param fileName   ?????????
     * @return file
     */
    public static File createFile(String folderPath, String fileName) {
        File destDir = new File(folderPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return new File(folderPath, fileName);
    }

    /**
     * ?????????????????????
     *
     * @param fileName ??????name
     * @return
     */
    public static String getFileFormat(String fileName) {
        if (TextUtils.isEmpty(fileName))
            return "";
        int point = 0;
        if (fileName.contains(".")) {
            point = fileName.lastIndexOf('.');
        } else {
            return "";
        }
        return fileName.substring(point + 1);
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param filePath ??????path
     * @return ?????????
     */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return "";
        return filePath.substring(filePath.lastIndexOf(File.separator) + 1);
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param filePath ??????path
     * @return ?????????
     */
    public static String getFileParentPath(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return "";
        return filePath.substring(0, filePath.lastIndexOf(File.separator));
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param filePath ??????path
     * @return file name
     */
    public static String getFileNameNoFormat(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }
        int point = filePath.lastIndexOf('.');
        return filePath.substring(filePath.lastIndexOf(File.separator) + 1,
                point);
    }


    /**
     * ??????????????????
     *
     * @param filePath ??????path
     * @return ????????????
     */
    public static long getFileSize(String filePath) {
        long size = 0;
        File file = new File(filePath);
        if (file != null && file.exists()) {
            size = file.length();
        }
        return size;
    }

    /**
     * ??????????????????
     *
     * @param fileSize ????????????
     * @return B/KB/MB/GB ??????????????????????????????
     */
    public static String formatFileSize(long fileSize) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.0");
        String fileSizeString = "";
        if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "KB";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "G";
        }
        return fileSizeString;
    }

    /**
     * ????????????????????????
     *
     * @param fileFolder ?????????
     * @return ??????????????????
     */
    public static long getDirSize(File fileFolder) {
        if (fileFolder == null) {
            return 0;
        }
        if (!fileFolder.isDirectory()) {
            return 0;
        }
        long dirSize = 0;
        File[] files = fileFolder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                dirSize += file.length();
            } else if (file.isDirectory()) {
                dirSize += file.length();
                dirSize += getDirSize(file); // ????????????????????????
            }
        }
        return dirSize;
    }

    /**
     * ????????????????????????
     *
     * @param fileFolder ?????????
     * @return ????????????
     */
    public long getFileList(File fileFolder) {
        long count = 0;
        File[] files = fileFolder.listFiles();
        count = files.length;
        for (File file : files) {
            if (file.isDirectory()) {
                count = count + getFileList(file);// ??????
                count--;
            }
        }
        return count;
    }

    /**
     * ??????????????? ???Android??????????????????????????? /data/data/PACKAGE_NAME/files ?????????
     *
     * @param context
     * @param fileName
     */
    public static void writeToDataFile(Context context, String fileName, String content) {
        if (content == null)
            content = "";
        try {
            FileOutputStream fos = context.openFileOutput(fileName,
                    Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String readDataFile(Context context, String fileName) {
        try {
            FileInputStream in = context.openFileInput(fileName);
            return readInStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * ??????????????????
     *
     * @param inStream ???????????????
     * @return
     */
    public static String readInStream(InputStream inStream) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int length = -1;
            while ((length = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, length);
            }
            outStream.close();
            inStream.close();
            return outStream.toString();
        } catch (IOException e) {
            Log.i("FileTest", e.getMessage());
        }
        return "";
    }

    /**
     * ?????????????????????????????????
     *
     * @param filePath
     * @return
     */
    public static String getFileString(String filePath) {
        File file = new File(filePath);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return readInStream(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * ????????????(?????????????????????????????????)
     *
     * @param filePath ??????path
     * @return ????????????
     */
    public static boolean deleteDirectory(String filePath) {
        boolean status;
        SecurityManager checker = new SecurityManager();
        if (!TextUtils.isEmpty(filePath)) {
            File newPath = new File(filePath);
            checker.checkDelete(newPath.toString());
            if (newPath.isDirectory()) {
                String[] listfile = newPath.list();
                try {
                    for (int i = 0; i < listfile.length; i++) {
                        File deletedFile = new File(newPath.toString() + "/"
                                + listfile[i].toString());
                        deletedFile.delete();
                    }
                    newPath.delete();
                    status = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    status = false;
                }

            } else
                status = false;
        } else
            status = false;
        return status;
    }

    /**
     * ????????????(?????????????????????????????????)
     *
     * @param filePath ??????path
     * @return ????????????
     */
    public static boolean deleteDirectoryFiles(String filePath) {
        boolean status;
        SecurityManager checker = new SecurityManager();
        if (!TextUtils.isEmpty(filePath)) {
            File newPath = new File(filePath);
            checker.checkDelete(newPath.toString());
            if (newPath.isDirectory()) {
                String[] listfile = newPath.list();
                try {
                    for (int i = 0; i < listfile.length; i++) {
                        File deletedFile = new File(newPath.toString() + "/"
                                + listfile[i].toString());
                        deletedFile.delete();
                    }
                    status = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    status = false;
                }

            } else
                status = false;
        } else
            status = false;
        return status;
    }

    /**
     * ????????????
     *
     * @param filePath ??????path
     * @return ????????????
     */
    public static boolean deleteFile(String filePath) {
        boolean status;
        SecurityManager checker = new SecurityManager();
        if (!TextUtils.isEmpty(filePath)) {
            File newPath = new File(filePath);
            checker.checkDelete(newPath.toString());
            if (newPath.isFile()) {
                try {
                    newPath.delete();
                    status = true;
                } catch (SecurityException se) {
                    se.printStackTrace();
                    status = false;
                }
            } else
                status = false;
        } else
            status = false;
        return status;
    }

    /**
     * ?????????
     *
     * @param oldPath ?????????
     * @param newPath ?????????
     * @return ??????
     */
    public static boolean reNamePath(String oldPath, String newPath) {
        File f = new File(oldPath);
        return f.renameTo(new File(newPath));
    }

    /**
     * ????????????
     *
     * @param filePath ??????Path
     */
    public static boolean deleteFileWithPath(String filePath) {
        SecurityManager checker = new SecurityManager();
        File file = new File(filePath);
        checker.checkDelete(filePath);
        if (file.isFile()) {
            file.delete();
            return true;
        }
        return false;
    }

    /**
     * ?????????????????????
     *
     * @param filePath path
     */
    public static void clearFileWithPath(String filePath) {
        List<File> files = listPathFiles(filePath);
        if (files.isEmpty()) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                clearFileWithPath(f.getAbsolutePath());
            } else {
                f.delete();
            }
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param root ?????????
     * @return
     */
    public static List<File> listPathFiles(String root) {
        List<File> allDir = new ArrayList<File>();
        SecurityManager checker = new SecurityManager();
        File path = new File(root);
        checker.checkRead(root);
        File[] files = path.listFiles();
        for (File f : files) {
            if (f.isFile())
                allDir.add(f);
            else
                listPath(f.getAbsolutePath());
        }
        return allDir;
    }

    /**
     * ??????root????????????????????????
     *
     * @param root ?????????
     * @return ????????????
     */
    public static List<String> listPath(String root) {
        List<String> allDir = new ArrayList<String>();
        SecurityManager checker = new SecurityManager();
        File path = new File(root);
        checker.checkRead(root);
        // ????????????.??????????????????
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory() && !f.getName().startsWith(".")) {
                    allDir.add(f.getAbsolutePath());
                }
            }
        }
        return allDir;
    }

    /**
     * ???url?????????file
     *
     * @param context context
     * @param uri     uri
     * @return
     */
    public static File getFileByUri(Context context, Uri uri) {
        String path = null;
        if ("file".equals(uri.getScheme())) {
            path = uri.getEncodedPath();
            if (path != null) {
                path = Uri.decode(path);
                ContentResolver cr = context.getContentResolver();
                StringBuffer buff = new StringBuffer();
                buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=").append("'" + path + "'").append(")");
                Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA}, buff.toString(), null, null);
                int index = 0;
                int dataIdx = 0;
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                    index = cur.getInt(index);
                    dataIdx = cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    path = cur.getString(dataIdx);
                }
                cur.close();
                if (index == 0) {
                } else {
                    Uri u = Uri.parse("content://media/external/images/media/" + index);
                    System.out.println("temp uri is :" + u);
                }
            }
            if (path != null) {
                return new File(path);
            }
        } else if ("content".equals(uri.getScheme())) {
            // 4.2.2??????
            String[] proj = {MediaStore.Images.Media.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                path = cursor.getString(columnIndex);
                cursor.close();
            }
            if (!TextUtils.isEmpty(path)) {
                return new File(path);
            }
        } else {
            return null;
        }
        return null;
    }

    /**
     * Android 10??????????????????????????????
     *
     * @param context
     * @param uri
     * @param type
     * @param maxSize 0???????????????
     * @return
     */
    public static Observable<String> sdkQUriToFilePath(final Context context, final Uri uri, final String type, final long maxSize) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                String sdAppFileDir = AndroidFileUtils.getSDAppFileDir(context, type);
                File fileByUri = getFileByUri(context, uri);
                String fileName = FileUtils.getFileName(fileByUri.getAbsolutePath());
                File file = new File(sdAppFileDir, fileName);
                FileUtils.writeToFile(file.getAbsolutePath(), inputStream);
                if (maxSize == 0) {
                    emitter.onNext(file.getAbsolutePath());
                    emitter.onComplete();
                } else {
                    String filePath = BitmapUtils.compressToAssignSize(context, file.getAbsolutePath(), maxSize);
                    emitter.onNext(filePath);
                    emitter.onComplete();
                }
            }
        });

    }


    /**
     * ????????????
     *
     * @param filePath ????????????
     * @param bytes    ??????
     */
    public static void saveFile(String filePath, byte[] bytes) {
        try {
            File file = new File(filePath);
            if (!file.exists())
                file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     *
     * @param filePath    ????????????
     * @param inputStream ???????????????
     * @return
     */
    public static boolean writeToFile(String filePath, InputStream inputStream) {
        int byteCount = 0;
        byte[] bytes = new byte[1024];
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath);
            while ((byteCount = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, byteCount);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * ??????asset???????????????????????????
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String readAssetData(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    /**
     * ???????????????
     *
     * @param filename
     * @return
     */
    public static byte[] readFile(String filename) throws FileNotFoundException {
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }
        FileChannel channel = null;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(f);
            channel = fs.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) channel.size());
            while ((channel.read(byteBuffer)) > 0) {
                // do nothing
            }
            return byteBuffer.array();
        } catch (IOException e) {
            e.printStackTrace();
            // throw e;
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fs.close();
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * ????????????
     *
     * @param srcPath ???????????????
     * @param tarPath ??????????????????
     * @return ????????????
     */
    public static boolean copyFile(String srcPath, String tarPath) {
        File srcFile = new File(srcPath);

        // ???????????????????????????
        if (!srcFile.exists()) {
            return false;
        } else if (!srcFile.isFile()) {
            return false;
        }
        // ??????????????????????????????
        File destFile = new File(tarPath);
        if (destFile.exists()) {
            // ???????????????????????????????????????

            // ?????????????????????????????????????????????????????????????????????????????????
            new File(tarPath).delete();

        } else {
            // ?????????????????????????????????????????????????????????
            if (!destFile.getParentFile().exists()) {
                // ?????????????????????????????????
                if (!destFile.getParentFile().mkdirs()) {
                    // ?????????????????????????????????????????????????????????
                    return false;
                }
            }
        }
        // ????????????
        int byteread = 0; // ??????????????????
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];

            while ((byteread = in.read(buffer)) != -1) {
                out.write(buffer, 0, byteread);
            }
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ????????????
     *
     * @param filePaths
     * @param zipFile
     * @return
     */
    public static Observable<String> zipFiles(final List<String> filePaths, final boolean deleteSrcFiles, final String zipFile) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                //??????ZIP
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
                for (String filePath : filePaths) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        ZipEntry zipEntry = new ZipEntry(FileUtils.getFileName(filePath));
                        FileInputStream inputStream = new FileInputStream(filePath);
                        zipOutputStream.putNextEntry(zipEntry);
                        int len;
                        byte[] buffer = new byte[4096];
                        while ((len = inputStream.read(buffer)) != -1) {
                            zipOutputStream.write(buffer, 0, len);
                        }
                        zipOutputStream.closeEntry();
                    }
                }
                //???????????????
                zipOutputStream.finish();
                zipOutputStream.close();
                if (deleteSrcFiles) {
                    for (String file : filePaths) {
                        deleteFile(file);
                    }
                }
                emitter.onNext(zipFile);
                emitter.onComplete();
            }
        });

    }


    /**
     * ????????????????????????
     *
     * @param srcFileString ??????????????????????????????
     * @param zipFileString ???????????????Zip??????
     */
    public static void ZipFolder(String srcFileString, String zipFileString) throws Exception {
        //??????ZIP
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));
        //????????????
        File file = new File(srcFileString);
        //??????
        ZipFiles(file.getParent() + File.separator, file.getName(), outZip);
        //???????????????
        outZip.finish();
        outZip.close();
    }

    /**
     * ????????????
     *
     * @param folderString
     * @param fileString
     * @param zipOutputSteam
     */
    private static void ZipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam) throws Exception {
        if (zipOutputSteam == null)
            return;
        File file = new File(folderString + fileString);
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(fileString);
            FileInputStream inputStream = new FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[4096];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputSteam.write(buffer, 0, len);
            }
            zipOutputSteam.closeEntry();
        } else {
            //?????????
            String fileList[] = file.list();
            //????????????????????????
            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                zipOutputSteam.putNextEntry(zipEntry);
                zipOutputSteam.closeEntry();
            }
            //??????????????????
            for (int i = 0; i < fileList.length; i++) {
                ZipFiles(folderString + fileString + "/", fileList[i], zipOutputSteam);
            }
        }
    }

    /**
     * ?????????????????????
     *
     * @param imageFormat
     * @return
     */
    public static String creatImageFileName(String imageFormat) {
        return "IMG" + DateUtil.formLocalTime("yyyyMMddhhmmss", System.currentTimeMillis()) + "." + imageFormat;
    }

    /**
     * ?????????????????????????????????
     *
     * @param filePath
     * @return
     */
    public static String readFileContent(String filePath) {
        String encoding = "utf-8";
        File file = new File(filePath);
        long fileLength = file.length();
        byte[] fileContent = new byte[(int) fileLength];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(fileContent);
            in.close();
        } catch (IOException e) {
//            GJLogger.logDiskAndConsole(e.getMessage());
            return null;
        }
        try {
            return new String(fileContent, encoding);
        } catch (UnsupportedEncodingException e) {
//            GJLogger.logDiskAndConsole(e.getMessage());
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param filePath ????????????
     * @param bytes    ??????
     */
    public static void writeFile(String filePath, byte[] bytes) {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(filePath);
            if (!file.exists())
                file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

}
