package com.evans.remindr;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.evans.remindr.db.Reminder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReminderActivity extends AppCompatActivity implements OnMapReadyCallback,
        SeekBar.OnSeekBarChangeListener {

    @Bind(R.id.textViewRadius) TextView radiusText;
    @Bind(R.id.seekBarRadius) AppCompatSeekBar radius;
    @Bind(R.id.locationInputLayout) TextInputLayout locationInputLayout;
    @Bind(R.id.locationEditText) EditText location;
    @Bind(R.id.remindersInputLayout) TextInputLayout remindersInputLayout;
    @Bind(R.id.remindersEditText) EditText reminders;
    @Bind(R.id.prioritySpinner) AppCompatSpinner priority;
    @Bind(R.id.setPeriod) AppCompatCheckBox setPeriod;
    @Bind(R.id.fromLayout) LinearLayout fromLayout;
    @Bind(R.id.toLayout) LinearLayout toLayout;

    @BindString(R.string.location_err_message) String locationErrMessage;
    @BindString(R.string.reminders_err_message) String remindersErrMessage;

    private Reminder mReminder;

    private GoogleMap mMap;
    private LatLng mPlace;
    private Circle mCircle;

    private DatePickerDialog mFromDatePickerDialog, mToDatePickerDialog;
    private TimePickerDialog mFromTimePickerDialog, mToTimePickerDialog;

    private Double mRadius = 0.0;

    private final int LOCATION_SPEECH_INPUT_CODE = 100;
    private final int REMINDERS_SPEECH_INPUT_CODE = 110;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        ButterKnife.bind(this);

        mReminder = (Reminder) getIntent().getSerializableExtra("reminder");
        mPlace = new LatLng(mReminder.getLatitude(), mReminder.getLongitude());

        radiusText.setText(String.valueOf(mRadius + 100));
        location.setText(mReminder.getLocation());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mReminder.getLocation());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        location.addTextChangedListener(new FormTextWatcher(location));
        reminders.addTextChangedListener(new FormTextWatcher(reminders));
        radius.setOnSeekBarChangeListener(this);

        DatePickerDialog.OnDateSetListener fromDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                TextView fromDate = (TextView) findViewById(R.id.fromDatePicker);
                fromDate.setText(year + "-" + monthOfYear + "-" + dayOfMonth);
            }
        };

        DatePickerDialog.OnDateSetListener toDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                TextView toDate = (TextView) findViewById(R.id.toDatePicker);
                toDate.setText(year + "-" + monthOfYear + "-" + dayOfMonth);
            }
        };

        TimePickerDialog.OnTimeSetListener fromTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                TextView fromTime = (TextView) findViewById(R.id.fromTimePicker);
                fromTime.setText(hourOfDay + ":" + minute);
            }
        };

        TimePickerDialog.OnTimeSetListener toTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                TextView toTime = (TextView) findViewById(R.id.toTimePicker);
                toTime.setText(hourOfDay + ":" + minute);
            }
        };

        Calendar calendar = Calendar.getInstance();
        mFromDatePickerDialog = new DatePickerDialog(this, fromDateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        mToDatePickerDialog = new DatePickerDialog(this, toDateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        mFromTimePickerDialog = new TimePickerDialog(this, fromTimeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        mToTimePickerDialog = new TimePickerDialog(this, toTimeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reminder_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_location:
                Toast.makeText(ReminderActivity.this, "Edit location", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_settings:
                Toast.makeText(ReminderActivity.this, "Settings", Toast.LENGTH_SHORT).show();
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
                .radius(100)
                .strokeWidth(1)
                .fillColor(ContextCompat.getColor(this, R.color.colorCircleFill))
                .strokeColor(ContextCompat.getColor(this, R.color.colorCircleStroke));
        mCircle = mMap.addCircle(circleOptions);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ButterKnife.bind(this);

        mRadius = (double) progress + 100;
        mCircle.setRadius(mRadius);
        radiusText.setText(String.valueOf(mRadius));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case LOCATION_SPEECH_INPUT_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> results = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    location.setText(results.get(0));
                }
                break;
            case REMINDERS_SPEECH_INPUT_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> results = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    reminders.setText(results.get(0));
                }
                break;
        }
    }

    @OnClick(R.id.locationVoiceInput)
    void locationSpeech() {
        promptSpeechInput(LOCATION_SPEECH_INPUT_CODE);
    }

    @OnClick(R.id.remindersVoiceInput)
    void remindersSppech() {
        promptSpeechInput(REMINDERS_SPEECH_INPUT_CODE);
    }

    @OnClick(R.id.setPeriod)
    void showDateLayout() {
        ButterKnife.bind(this);
        if (setPeriod.isChecked()) {
            fromLayout.setVisibility(View.VISIBLE);
            toLayout.setVisibility(View.VISIBLE);
        } else {
            fromLayout.setVisibility(View.GONE);
            toLayout.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.fromDatePicker)
    void fromPickDate() {
        mFromDatePickerDialog.show();
    }

    @OnClick(R.id.toDatePicker)
    void toPickDate() {
        mToDatePickerDialog.show();
    }

    @OnClick(R.id.fromTimePicker)
    void fromPickTime() {
        mFromTimePickerDialog.show();
    }

    @OnClick(R.id.toTimePicker)
    void toPickTime() {
        mToTimePickerDialog.show();
    }

    @OnClick(R.id.saveButton)
    void saveReminder() {
        ButterKnife.bind(this);

        if (validateLocationText() && validateRemindersText()) {
            mReminder.setLocation(location.getText().toString().trim());
            mReminder.setRadius(mRadius);
            mReminder.setAlarm(1);

            String priorityText = priority.getSelectedItem().toString();
            switch (priorityText) {
                case "Normal priority":
                    mReminder.setPriority("normal");
                    break;
                case "High priority":
                    mReminder.setPriority("high");
                    break;
                case "Low priority":
                    mReminder.setPriority("low");
                    break;
            }

            mReminder.setReminders(reminders.getText().toString().trim());
            mReminder.setPeriod(setPeriod.isChecked() ? 1 : 0);

            final Date date = new Date();

            mReminder.setFromDate(date.getTime());
            mReminder.setToDate(date.getTime());
            mReminder.setCreated(date.getTime());
            mReminder.setUpdated(date.getTime());

            if (mReminder.save() > -1) {
                Toast.makeText(ReminderActivity.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ReminderActivity.this, "Error occured while saving", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @OnClick(R.id.cancelButton)
    void cancelReminder() {
        new MaterialDialog.Builder(this)
                .content("Discard changes made?")
                .positiveText("DISCARD")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
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
    }

    private void promptSpeechInput(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ReminderActivity.this, "Speech input not supported", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private boolean validateLocationText() {
        ButterKnife.bind(this);

        if (location.getText().toString().trim().isEmpty()) {
            locationInputLayout.setError(locationErrMessage);
            requestFocus(location);
            return false;
        } else {
            locationInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateRemindersText() {
        ButterKnife.bind(this);

        if (reminders.getText().toString().trim().isEmpty()) {
            remindersInputLayout.setError(remindersErrMessage);
            requestFocus(reminders);
            return false;
        } else {
            remindersInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private class FormTextWatcher implements TextWatcher {
        private View view;

        private FormTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (view.getId()) {
                case R.id.locationEditText:
                    validateLocationText();
                    break;
                case R.id.remindersEditText:
                    validateRemindersText();
                    break;
            }
        }
    }
}
