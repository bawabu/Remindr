package com.evans.remindr.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evans.remindr.MainActivity;
import com.evans.remindr.R;
import com.evans.remindr.ReminderDetailActivity;
import com.evans.remindr.db.Reminder;

import java.util.List;

/**
 * Populate database data to the RecyclerView.
 *
 * Created by evans on 3/20/16.
 */
public class ReminderListAdapter extends RecyclerView.Adapter<ReminderListAdapter.ViewHolder> {

    private Context mContext;
    private List<Reminder> mReminders;

    public ReminderListAdapter(Context context, List<Reminder> reminders) {
        this.mContext = context;
        this.mReminders = reminders;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView location, remindersText;
        public SwitchCompat alarm;

        public ViewHolder(View view) {
            super(view);

            location = (TextView) view.findViewById(R.id.textViewLocation);
            remindersText = (TextView) view.findViewById(R.id.textViewReminders);
            alarm = (SwitchCompat) view.findViewById(R.id.switchCompatAlarm);

            alarm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Reminder reminder = mReminders.get(getLayoutPosition());
                    reminder.setAlarm(alarm.isChecked() ? 1 : 0);
                    reminder.save();
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Reminder reminder = mReminders.get(getLayoutPosition());

                    Intent intent = new Intent(mContext, ReminderDetailActivity.class);
                    intent.putExtra("reminder", reminder);
                    mContext.startActivity(intent);
                }
            });
        }
    }

    @Override
    public ReminderListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reminder_row, parent,
                false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReminderListAdapter.ViewHolder holder, int position) {
        final Reminder reminder = mReminders.get(position);
        int backgroundResource;

        switch (reminder.getPriority()) {
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

        holder.itemView.setBackgroundResource(backgroundResource);
        holder.location.setText(reminder.getLocation());
        holder.remindersText.setText(reminder.getReminders());
        holder.alarm.setChecked(reminder.getAlarm() == 1);
    }

    @Override
    public int getItemCount() {
        return mReminders.size();
    }
}
