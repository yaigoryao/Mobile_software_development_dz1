package ru.msfd.common;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.util.UUID;

public class AlarmData implements Parcelable {
    private LocalTime alarmTime;
    private boolean isActivated = false;
    private int soundId = -1;
    private UUID alarmUUID = UUID.randomUUID();

    protected AlarmData(Parcel in) {
        this.isActivated = (in.readInt() != 0);
        String timeString = in.readString();
        if (timeString != null) this.alarmTime = LocalTime.parse(timeString);
        else this.alarmTime = LocalTime.now();
        this.soundId = in.readInt();
        this.alarmUUID = UUID.fromString(in.readString());
    }

    public static final Creator<AlarmData> CREATOR = new Creator<AlarmData>() {
        @Override
        public AlarmData createFromParcel(Parcel in) {
            return new AlarmData(in);
        }

        @Override
        public AlarmData[] newArray(int size) {
            return new AlarmData[size];
        }
    };

    public LocalTime getAlarmTime() { return this.alarmTime; }
    public boolean getIsActivated() { return this.isActivated; }
    public int getSoundId() { return this.soundId; }
    public UUID getAlarmUUID() { return this.alarmUUID; }

    public void setAlarmTime(LocalTime localTime) { if(localTime != null && !localTime.equals(this.alarmTime)) this.alarmTime = localTime; }
    public void setIsActivated(boolean isActivated) { if(this.isActivated != isActivated) this.isActivated = isActivated; }
    public void setSoundId(int soundId) { if(this.soundId != soundId) this.soundId = soundId; }
    public void setAlarmUUID(UUID uuid) { if(this.alarmUUID != null && !this.alarmUUID.equals(uuid)) this.alarmUUID = uuid; }

    public AlarmData()
    {
        this(false, LocalTime.now(), -1, UUID.randomUUID());
    }

    public AlarmData(String string)
    {
        AlarmData data = fromString(string);
        if(data != null)
        {
            setIsActivated(data.getIsActivated());
            setAlarmTime(data.getAlarmTime());
            setSoundId(data.getSoundId());
            setAlarmUUID(data.getAlarmUUID());
        }
    }

    public AlarmData(boolean isActivated, LocalTime localTime, int soundId, UUID uuid)
    {
        setIsActivated(isActivated);
        setAlarmTime(localTime);
        setSoundId(soundId);
        setAlarmUUID(uuid != null ? uuid : UUID.randomUUID());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(this.isActivated ? 1 : 0);
        parcel.writeString(this.alarmTime.toString());
        parcel.writeInt(this.soundId);
        parcel.writeString(this.alarmUUID.toString());
    }

    public static AlarmData fromString(String input)
    {
        try
        {
            if(input == null) return null;
            String[] data = input.split(";");
            if(data.length != 4) return null;
            return new AlarmData(Integer.parseInt(data[0]) != 0, LocalTime.parse(data[1]), Integer.parseInt(data[2]), UUID.fromString(data[3]));
        }
        catch(Exception e)
        {
            return null;
        }
    }

    @Override
    public String toString() { return (this.isActivated ? 1 : 0) + ";" + this.alarmTime.toString() + ";" + this.soundId + ";" + this.alarmUUID.toString(); }
}
