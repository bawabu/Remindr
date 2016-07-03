package com.evans.remindr;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.evans.remindr.adapter.ReminderListAdapter;
import com.evans.remindr.db.Reminder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.orm.SugarContext;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    @Bind(R.id.progressBar)
    ProgressBar progressBar;

    int PLACE_PICKER_REQUEST = 1;

    private RecyclerView mRecyclerView;
    private List<Reminder> mReminders;
    private ReminderListAdapter mAdapter;

    private static final long UPDATE_INTERVAL_IN_MS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL_IN_MS / 2;

    protected final static String LOCATION_KEY = "location-key";

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SugarContext.init(this);
        ButterKnife.bind(this);

        FloatingActionButton fabPickLocation = (FloatingActionButton)
                findViewById(R.id.fabPickLocation);
        fabPickLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MainActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException |
                        GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        setupToolbar();

        setupRecyclerView();

        updateValuesFromBundle(savedInstanceState);

        buildGoogleApiClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchReminders();

        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mLocation = savedInstanceState.getParcelable(LOCATION_KEY);
                showToast();
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void showToast() {
        if (mReminders != null) {
            for (Reminder reminder : mReminders) {
                float[] results = new float[3];
                Location.distanceBetween(mLocation.getLatitude(), mLocation.getLongitude(),
                        reminder.getLatitude(), reminder.getLongitude(), results);
                if (results.length > 0) {
                    float distance = results[0];

                    if (distance <= reminder.getRadius() && reminder.getAlarm() == 1) {
                        Log.d("LOCATION", "Reached=> Location: " + reminder.getLocation() + ", Radius: " + distance);
                        showNotification(reminder);
                    }
                }
            }
        }
    }

    private void showNotification(Reminder reminder) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_add_location_white_24px)
                .setContentTitle(reminder.getLocation())
                .setContentText(reminder.getReminders());

        Intent intent = new Intent(this, ReminderDetailActivity.class);
        intent.putExtra("reminder", reminder);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ReminderDetailActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        progressBar.setVisibility(View.VISIBLE);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Reminder reminder = new Reminder();
                Place place = PlacePicker.getPlace(this, data);
                reminder.setLocation(String.valueOf(place.getName()));
                reminder.setLatitude(place.getLatLng().latitude);
                reminder.setLongitude(place.getLatLng().longitude);
                Intent intent = new Intent(this, ReminderActivity.class);
                intent.putExtra("reminder", reminder);
                progressBar.setVisibility(View.GONE);
                startActivity(intent);
            }
            progressBar.setVisibility(View.GONE);
        }
        progressBar.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(R.string.app_name);
    }

    private void setupRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.rViewReminders);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                final int itemPosition = parent.getChildAdapterPosition(view);
                if (itemPosition != RecyclerView.NO_POSITION) {
                    final int itemCount = state.getItemCount();

                    if (itemCount > 0 && itemPosition == itemCount - 1)
                        outRect.set(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.last_child));
                    else
                        outRect.set(0, 0, 0, 0);
                }
            }
        });

        fetchReminders();
    }

    private void fetchReminders() {
        long remindersCount = Reminder.count(Reminder.class);

        if (remindersCount > 0) {
            mReminders = Reminder.listAll(Reminder.class, "id DESC");
            mAdapter = new ReminderListAdapter(this, mReminders);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            Snackbar.make(mRecyclerView, "No reminders added.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mLocation == null) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            showToast();
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        showToast();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("LOCATION", "Connection failed");
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mLocation);
        super.onSaveInstanceState(savedInstanceState);
    }
}
