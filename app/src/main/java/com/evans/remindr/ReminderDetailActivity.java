package com.evans.remindr;

import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.evans.remindr.db.Reminder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ReminderDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.bottomSheet) LinearLayout linearLayout;
    @Bind(R.id.locationTextView) TextView location;
    @Bind(R.id.radiusTextView) TextView radius;
    @Bind(R.id.remindersTextView) TextView reminders;
    @Bind(R.id.alarmTextView) TextView alarm;
    @Bind(R.id.locationLinearLayout) LinearLayout locationLinearLayout;

    private Reminder mReminder;

    private GoogleMap mMap;
    private LatLng mPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_detail);
        ButterKnife.bind(this);

        mReminder = (Reminder) getIntent().getSerializableExtra("reminder");
        mPlace = new LatLng(mReminder.getLatitude(), mReminder.getLongitude());

        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mReminder.getLocation());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(false);
        mapFragment.getMapAsync(this);

        BottomSheetBehavior<LinearLayout>  sheetBehavior = BottomSheetBehavior.from(linearLayout);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                Toast.makeText(ReminderDetailActivity.this, "State changed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });

        location.setText(mReminder.getLocation());
        radius.setText("Radius: " + String.valueOf(mReminder.getRadius()) + " meters");
        reminders.setText(mReminder.getReminders());
        alarm.setText("Alarm: " + ((mReminder.getAlarm() == 1) ? "ON" : "OFF"));
        setBackgroundResource();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reminder_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_reminder:
                Toast.makeText(ReminderDetailActivity.this, "Edit reminder", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_delete_reminder:
                new MaterialDialog.Builder(this)
                        .content("Delete '" + mReminder.getLocation() + "' reminders?")
                        .positiveText("DELETE")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                mReminder.delete();
                                finish();
                            }
                        })
                        .negativeText("Cancel")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.action_settings:
                Toast.makeText(ReminderDetailActivity.this, "Settings", Toast.LENGTH_SHORT).show();
                break;
        }

        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        addMarker();
        addCircle();
    }

    private void addMarker() {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(mPlace);
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mPlace, 15));
    }

    private void addCircle() {
        CircleOptions circleOptions = new CircleOptions()
                .center(mPlace)
                .radius(mReminder.getRadius())
                .strokeWidth(1)
                .fillColor(ContextCompat.getColor(this, R.color.colorCircleFill))
                .strokeColor(ContextCompat.getColor(this, R.color.colorCircleStroke));
        mMap.addCircle(circleOptions);
    }

    private void setBackgroundResource() {
        int backgroundResource;
        switch (mReminder.getPriority()) {
            case "high":
                backgroundResource = R.drawable.layout_border_high;
                break;
            case "normal":
                backgroundResource = R.drawable.layout_border_normal;
                break;
            case "low":
                backgroundResource = R.drawable.layout_border_low;
                break;
            default:
                backgroundResource = R.drawable.layout_border_normal;
                break;
        }

        locationLinearLayout.setBackgroundResource(backgroundResource);
    }
}
