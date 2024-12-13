package ru.msfd.common;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ru.msfd.dz1.AlarmManager;
import ru.msfd.dz1.AlarmService;
import ru.msfd.dz1.R;

public class AlarmListItemAdapter extends RecyclerView.Adapter<AlarmListViewHolder> {
    private ArrayList<AlarmData> items;
    private OnItemClickListener onItemClickListener;
    private Context context;

    private void UpdateAlarmData(AlarmData alarmData)
    {
        Log.d("Adapter", "Обновление данных будильника");
        SharedPreferences preferences = context.getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(alarmData.getAlarmUUID().toString(), alarmData.toString());
        //editor.clear();
        //for (AlarmData alarmData : items) editor.putString(alarmData.getAlarmUUID().toString(), alarmData.toString());
        editor.commit();
    }

    public AlarmListItemAdapter(ArrayList<AlarmData> items, OnItemClickListener onItemClickListener, Context context)
    {
        this.items = items;
        this.onItemClickListener = onItemClickListener;
        this.context = context;
    }

    public interface OnItemClickListener
    {
        void onItemClick(int position);
    }

    @Override
    public AlarmListViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new AlarmListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.alarms_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(AlarmListViewHolder holder, int position) {
        AlarmData alarmData = items.get(position);
        LocalTime alarmTime = alarmData.getAlarmTime();

        holder.alarmTimePicker.setIs24HourView(true);

        holder.alarmToggleButton.setOnCheckedChangeListener(null);
        holder.alarmToggleButton.setChecked(alarmData.getIsActivated());
        holder.alarmTimePicker.setHour(alarmTime.getHour());
        holder.alarmTimePicker.setMinute(alarmTime.getMinute());

        ArrayList<SoundInfo> soundsInfo = this.getAlarmSoundsInfo();
        ArrayAdapter<SoundInfo> adapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, soundsInfo);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.alarmSoundSpinner.setAdapter(adapter);
        holder.alarmSoundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION)
                {
                    items.get(currentPosition).setSoundId(soundsInfo.get(i).getId());
                    items.get(currentPosition).setIsActivated(false);
                    UpdateAlarmData(items.get(currentPosition));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION)
                {
                    int index = soundsInfo.indexOf(soundsInfo.stream().filter(i -> Objects.equals(i.getId(), items.get(currentPosition).getSoundId())).findFirst().orElse(null));
                    holder.alarmSoundSpinner.setSelection(index == -1 ? 0 : index);
                }
            }
        });

        int currentPos = holder.getAdapterPosition();
        if (currentPos != RecyclerView.NO_POSITION)
        {
            int itemIndex = soundsInfo.indexOf(soundsInfo.stream().filter(i -> Objects.equals(i.getId(), items.get(currentPos).getSoundId())).findFirst().orElse(null));
            holder.alarmSoundSpinner.setSelection(itemIndex == -1 ? 0 : itemIndex);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Log.d("Adapter", "Выбрана позиция " + currentPosition);
                    Log.d("Adapter", "Выбран будильник " + items.get(currentPosition).toString());
                    onItemClickListener.onItemClick(currentPosition);
                }
            }
        });

        holder.alarmToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION)
                    items.get(currentPosition).setIsActivated(isChecked);
                UpdateAlarmData(items.get(currentPosition));
                Intent alarmIntent = new Intent(holder.itemView.getContext(), AlarmService.class);
                //Log.d("Service Intent", items.get(currentPosition).toString());
                //Log.d("Service Intent", String.valueOf(currentPosition) + " позиция в списке");
                alarmIntent.putExtra(AlarmManager.ALARM_DATA_EXTRAS, items.get(currentPosition).toString());
                //alarmIntent.putExtra(AlarmManager.ALARM_ARRAY_POS, currentPosition);
                //holder.itemView.getContext().startService(alarmIntent);
                holder.itemView.getContext().startForegroundService(alarmIntent);
            }
        });

        holder.alarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hours, int minutes) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION)
                {
                    items.get(currentPosition).setAlarmTime(LocalTime.of(hours, minutes));
                    items.get(currentPosition).setIsActivated(false);
                    UpdateAlarmData(items.get(currentPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private ArrayList<SoundInfo> getAlarmSoundsInfo() {
        ArrayList<SoundInfo> soundsInfo = new ArrayList<>();
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Cursor cursor = null;
            try
            {
                cursor = context.getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{ MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE },null, null, null );
                if (cursor != null)
                {
                    int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    if(titleIndex != -1 && idIndex != -1)
                    {
                        while (cursor.moveToNext())
                            soundsInfo.add(new SoundInfo(cursor.getString(titleIndex), cursor.getInt(idIndex)));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (cursor != null) cursor.close();
            }
        }
        else Toast.makeText(context, "Разрешение было отклонено пользователем", Toast.LENGTH_LONG).show();
        return soundsInfo;
    }
}
