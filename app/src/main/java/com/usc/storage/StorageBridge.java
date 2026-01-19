package com.usc.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import androidx.documentfile.provider.DocumentFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import android.util.SparseArray;

/**
 * Lightweight StorageBridge: persists tree URI and provides open/read/write/close
 * handles for native to use when path uses the special saf:/ prefix.
 */
public class StorageBridge {
    private static final String PREF_TREE_URI = "saf_tree_uri";
    private static Context sContext = null;
    private static final SparseArray<Holder> sHandles = new SparseArray<>();
    private static int sNextHandle = 1;

    private static class Holder {
        ParcelFileDescriptor pfd;
        InputStream in;
        OutputStream out;
    }

    public static void init(Context ctx) {
        sContext = ctx.getApplicationContext();
    }

    public static boolean setTreeUri(String treeUri) {
        if (sContext == null || treeUri == null)
            return false;
        Uri uri = Uri.parse(treeUri);
        try {
            final int takeFlags = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            sContext.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Exception ignored) { }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sContext);
        prefs.edit().putString(PREF_TREE_URI, treeUri).apply();
        return true;
    }

    private static String getTreeUri() {
        if (sContext == null) return null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sContext);
        return prefs.getString(PREF_TREE_URI, null);
    }

    private static Uri resolveSafPath(String safPath, boolean createIfMissing, boolean forDirectory) {
        String tree = getTreeUri();
        if (tree == null) return null;
        Uri treeUri = Uri.parse(tree);
        DocumentFile root = DocumentFile.fromTreeUri(sContext, treeUri);
        if (root == null) return null;
        String rel = safPath;
        if (rel.startsWith("saf:/"))
            rel = rel.substring(5);
        if (rel.startsWith("/"))
            rel = rel.substring(1);
        if (rel.isEmpty()) return root.getUri();
        String[] parts = rel.split("/");
        DocumentFile cur = root;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            DocumentFile next = cur.findFile(p);
            boolean last = (i == parts.length - 1);
            if (next == null) {
                if (!createIfMissing) return null;
                if (last && !forDirectory) {
                    next = cur.createFile("application/octet-stream", p);
                } else {
                    next = cur.createDirectory(p);
                }
                if (next == null) return null;
            }
            cur = next;
        }
        return cur.getUri();
    }

    public static int openResolved(String safPath, boolean write) {
        if (sContext == null) return -1;
        try {
            Uri docUri = resolveSafPath(safPath, write, false);
            if (docUri == null) return -1;
            ParcelFileDescriptor pfd = sContext.getContentResolver().openFileDescriptor(docUri, write ? "rw" : "r");
            if (pfd == null) return -1;
            Holder h = new Holder();
            h.pfd = pfd;
            if (!write) {
                h.in = new FileInputStream(pfd.getFileDescriptor());
            } else {
                h.out = new FileOutputStream(pfd.getFileDescriptor());
            }
            int handle = sNextHandle++;
            sHandles.put(handle, h);
            return handle;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int read(int handle, byte[] buf, int len) {
        Holder h = sHandles.get(handle);
        if (h == null || h.in == null) return -1;
        try {
            int toRead = Math.min(len, buf.length);
            int r = h.in.read(buf, 0, toRead);
            return r >= 0 ? r : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int write(int handle, byte[] buf, int len) {
        Holder h = sHandles.get(handle);
        if (h == null || h.out == null) return -1;
        try {
            h.out.write(buf, 0, len);
            return len;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void close(int handle) {
        Holder h = sHandles.get(handle);
        if (h == null) return;
        try { if (h.in != null) h.in.close(); } catch (Exception ignored) {}
        try { if (h.out != null) h.out.flush(); } catch (Exception ignored) {}
        try { if (h.out != null) h.out.close(); } catch (Exception ignored) {}
        try { if (h.pfd != null) h.pfd.close(); } catch (Exception ignored) {}
        sHandles.remove(handle);
    }

    public static native void nativeSetGameDir(String path);