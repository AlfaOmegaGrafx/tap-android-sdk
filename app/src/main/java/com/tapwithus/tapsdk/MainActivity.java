package com.tapwithus.tapsdk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.tapwithus.sdk.bluetooth.MousePacket;
import com.tapwithus.sdk.tap.Tap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TapSdk sdk;
    private RecyclerViewAdapter adapter;
    private boolean startWithControllerMode = true;
    private String lastConnectedTapAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log("onCreate");


        sdk = TapSdkFactory.getDefault(this);
        sdk.enableDebug();
        if (!startWithControllerMode) {
            sdk.disableAutoSetControllerModeOnConnection();
        }
        sdk.registerTapListener(tapListener);
        if (sdk.isConnectionInProgress()) {
            log("A Tap is connecting");
        }

        initRecyclerView();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                sdk.setMouseNotification("D7:A9:E0:8C:17:6E");
//                String tapIdentifier = "D7:A9:E0:8C:17:6E";
//                sdk.writeName(tapIdentifier, "YanivWithCase");

//                sdk.restartBluetooth();
                sdk.refreshBond(lastConnectedTapAddress);
            }
        });
    }

    private TapListItemOnClickListener itemOnClickListener = new TapListItemOnClickListener() {
        @Override
        public void onClick(TapListItem item) {
            if (item.isInControllerMode) {
                log("Switching to TEXT mode");
                sdk.startMode(item.tapIdentifier, TapSdk.MODE_TEXT);
            } else {
                log("Switching to CONTROLLER mode");
                sdk.startMode(item.tapIdentifier, TapSdk.MODE_CONTROLLER);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        log("onResume");

        Set<String> connectedTaps = sdk.getConnectedTaps();
        List<TapListItem> listItems = new ArrayList<>();
        for (String tapIdentifier: connectedTaps) {
            TapListItem tapListItem = new TapListItem(tapIdentifier, itemOnClickListener);
            tapListItem.isInControllerMode = sdk.isInMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
            listItems.add(tapListItem);
        }
        adapter.updateList(listItems);

        sdk.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        log("onPause");
        sdk.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        log("onDestroy");
        sdk.unregisterTapListener(tapListener);
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sdk.close();
                        int pid = android.os.Process.myPid();
                        android.os.Process.killProcess(pid);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        List<TapListItem> listItems = new ArrayList<>();

        adapter = new RecyclerViewAdapter(listItems);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    private TapListener tapListener = new TapListener() {

        @Override
        public void onBluetoothTurnedOn() {
            log("Bluetooth turned ON");
        }

        @Override
        public void onBluetoothTurnedOff() {
            log("Bluetooth turned OFF");
        }

        @Override
        public void onTapStartConnecting(@NonNull String tapIdentifier) {
            log("Tap started connecting - " + tapIdentifier);
        }

        @Override
        public void onTapConnected(@NonNull String tapIdentifier) {
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            lastConnectedTapAddress = tapIdentifier;
            log("TAP connected " + tap.toString());

            adapter.removeItem(tapIdentifier);

            TapListItem newItem = new TapListItem(tapIdentifier, itemOnClickListener);
            newItem.tapName = tap.getName();
            newItem.tapFwVer = tap.getFwVer();
            newItem.isInControllerMode = sdk.isInMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
            adapter.addItem(newItem);
        }

        @Override
        public void onTapDisconnected(@NonNull String tapIdentifier) {
            log("TAP disconnected " + tapIdentifier);
            adapter.removeItem(tapIdentifier);
        }

        @Override
        public void onTapResumed(@NonNull String tapIdentifier) {
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            log("TAP resumed " + tap);
            adapter.updateFwVer(tapIdentifier, tap.getFwVer());
        }

        @Override
        public void onTapChanged(@NonNull String tapIdentifier) {
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            log("TAP changed " + tap);
            adapter.updateFwVer(tapIdentifier, tap.getFwVer());
        }

        @Override
        public void onControllerModeStarted(@NonNull String tapIdentifier) {
            log("Controller mode started " + tapIdentifier);
            adapter.onControllerModeStarted(tapIdentifier);
        }

        @Override
        public void onTextModeStarted(@NonNull String tapIdentifier) {
            log("Text mode started " + tapIdentifier);
            adapter.onTextModeStarted(tapIdentifier);
        }

        @Override
        public void onTapInputReceived(@NonNull String tapIdentifier, int data) {
            adapter.updateTapInput(tapIdentifier, data);
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {
//            log(tapIdentifier + " MOUSE input received " + data.dx.getInt() + " " + data.dy.getInt() + " " + data.dt.getUnsignedLong());
        }

        @Override
        public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {
            log("Error - " + tapIdentifier + " - " + code + " - " + description);
        }
    };

    private void log(String message) {
        Log.e(this.getClass().getSimpleName(), message);
    }
}
