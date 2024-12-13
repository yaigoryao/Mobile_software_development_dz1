package ru.msfd.common;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import androidx.recyclerview.widget.RecyclerView;

import ru.msfd.dz1.R;

public class AlarmListViewHolder extends RecyclerView.ViewHolder {
    ToggleButton alarmToggleButton;
    TimePicker alarmTimePicker;
    Spinner alarmSoundSpinner;

    public AlarmListViewHolder(View alarmsListItem)
    {
        super(alarmsListItem);
        alarmToggleButton = (ToggleButton) alarmsListItem.findViewById(R.id.alarm_toggle_button);
        alarmTimePicker = (TimePicker) alarmsListItem.findViewById(R.id.alarm_time_picker);
        alarmSoundSpinner = (Spinner) alarmsListItem.findViewById(R.id.alarm_sound_spinner);
    }
}
