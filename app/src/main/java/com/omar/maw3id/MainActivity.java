package com.omar.maw3id;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_FILES = 1;
    private static final int SAVE_ONE = 2;
    private static final int SAVE_MANY = 3;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingBytes;
    private String pendingJson;

    public class Bridge {
        @JavascriptInterface
        public void saveFile(final String name, final String mime, final String b64) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        pendingBytes = Base64.decode(b64, Base64.DEFAULT);
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType(mime);
                        i.putExtra(Intent.EXTRA_TITLE, name);
                        startActivityForResult(i, SAVE_ONE);
                    } catch (Exception e) {
                        notifySaved(false);
                    }
                }
            });
        }

        @JavascriptInterface
        public void setReminders(String json) {
            Reminders.setAll(getApplicationContext(), json);
        }

        @JavascriptInterface
        public void askNotif() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= 33
                            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
                    }
                }
            });
        }

        @JavascriptInterface
        public void openFile(final String name, final String mime, final String b64) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT < 29) {
                        saveFile(name, mime, b64);
                        return;
                    }
                    try {
                        ContentValues v = new ContentValues();
                        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                        v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Maw3id");
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                        OutputStream out = getContentResolver().openOutputStream(uri);
                        out.write(Base64.decode(b64, Base64.DEFAULT));
                        out.close();
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        view.setDataAndType(uri, mime);
                        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(view, name));
                    } catch (Exception e) {
                        web.evaluateJavascript("window.toast2&&toast2('Ma l9itch app t7el had l-fichier')", null);
                    }
                }
            });
        }

        @JavascriptInterface
        public void saveFiles(final String json) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        pendingJson = json;
                        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SAVE_MANY);
                    } catch (Exception e) {
                        notifySaved(false);
                    }
                }
            });
        }
    }

    private void notifySaved(boolean ok) {
        if (web != null) {
            web.evaluateJavascript("window.dftarSaved&&window.dftarSaved(" + ok + ")", null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "DftarAndroid");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;
                boolean images = false;
                String[] accept = params.getAcceptTypes();
                if (accept != null) {
                    for (String a : accept) {
                        if (a != null && a.startsWith("image")) {
                            images = true;
                        }
                    }
                }
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(images ? "image/*" : "*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(intent, images ? "Khtar tswira" : "Khtar fichier"), PICK_FILES);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });
        setContentView(web);
        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILES) {
            if (fileCallback == null) {
                return;
            }
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                ClipData clip = data.getClipData();
                if (clip != null) {
                    result = new Uri[clip.getItemCount()];
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        result[i] = clip.getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        if (requestCode == SAVE_ONE) {
            boolean ok = false;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingBytes != null) {
                try {
                    OutputStream out = getContentResolver().openOutputStream(data.getData());
                    out.write(pendingBytes);
                    out.close();
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            pendingBytes = null;
            notifySaved(ok);
            return;
        }
        if (requestCode == SAVE_MANY) {
            boolean ok = false;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingJson != null) {
                try {
                    Uri tree = data.getData();
                    Uri dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
                    JSONArray files = new JSONArray(pendingJson);
                    for (int i = 0; i < files.length(); i++) {
                        JSONObject f = files.getJSONObject(i);
                        Uri doc = DocumentsContract.createDocument(getContentResolver(), dir, f.getString("mime"), f.getString("name"));
                        if (doc == null) {
                            continue;
                        }
                        OutputStream out = getContentResolver().openOutputStream(doc);
                        out.write(Base64.decode(f.getString("b64"), Base64.DEFAULT));
                        out.close();
                    }
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            pendingJson = null;
            notifySaved(ok);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
