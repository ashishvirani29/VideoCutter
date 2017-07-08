package com.veeradeveloper.videocutter;

public abstract class AbstractSerializedData {

    public abstract void writeInt32(int x);

    public abstract void writeInt64(long x);

    public abstract void writeBool(boolean value);

    public abstract void writeString(String s);

    public abstract void writeByteArray(byte[] b);

    public abstract void writeDouble(double d);

    public abstract int readInt32(boolean exception);

    public abstract boolean readBool(boolean exception);

    public abstract long readInt64(boolean exception);

    public abstract String readString(boolean exception);

    public abstract byte[] readByteArray(boolean exception);

    public abstract double readDouble(boolean exception);

    public abstract int length();

    public abstract int getPosition();
}
