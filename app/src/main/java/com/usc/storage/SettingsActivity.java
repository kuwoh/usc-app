package com.usc.storage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class SettingsActivity extends Activity {
    private static final int REQUEST_OPEN_TREE = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button b = new Button(this);
        b.setText("Pick storage folder (Documents)");
        b.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_OPEN_TREE);
        });
        setContentView(b);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_TREE && resultCode == Activity.RESULT_OK) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                final int takeFlags = (data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                boolean ok = StorageBridge.setTreeUri(treeUri.toString());
                StorageBridge.nativeSetGameDir("saf:/");
                Toast.makeText(this, ok ? "Storage folder set" : "Failed to set folder", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}