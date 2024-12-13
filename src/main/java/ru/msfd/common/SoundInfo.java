package ru.msfd.common;

public class SoundInfo {
    public static final String NO_SOUND = "NO SOUND";
    public static final int NO_SOUND_ID = -1;

    private String name = null;
    private int id = -1;

    public String getName() { return this.name; };
    public int getId() { return this.id; };

    public void setName(String name) { if (name != null && !name.equals(this.name)) this.name = name; }
    public void setId(int id) { if (this.id != id) this.id = id; }

    public SoundInfo(String name, int id)
    {
        this.setName(name);
        this.setId(id);
    }

    public SoundInfo()
    {
        this(NO_SOUND, NO_SOUND_ID);
    }

    @Override
    public String toString() { return this.name; }
}
