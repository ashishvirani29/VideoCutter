package com.veeradeveloper.videocutter;

import android.util.Log;


import java.nio.ByteBuffer;

public class NativeByteBuffer extends AbstractSerializedData {

    public ByteBuffer buffer;
    private boolean justCalc;
    private int len;

    public void position(int position) {
        buffer.position(position);
    }

    public void writeInt32(int x) {
        try {
            if (!justCalc) {
                buffer.putInt(x);
            } else {
                len += 4;
            }
        } catch(Exception e) {
            Log.e("tmessages", "write int32 error");
        }
    }

    public void writeInt64(long x) {
        try {
            if (!justCalc) {
                buffer.putLong(x);
            } else {
                len += 8;
            }
        } catch(Exception e) {
            Log.e("tmessages", "write int64 error");
        }
    }

    public void writeBool(boolean value) {
        if (!justCalc) {
            if (value) {
                writeInt32(0x997275b5);
            } else {
                writeInt32(0xbc799737);
            }
        } else {
            len += 4;
        }
    }

    public void writeString(String s) {
        try {
            writeByteArray(s.getBytes("UTF-8"));
        } catch(Exception e) {
            Log.e("tmessages", "write string error");
        }
    }

    public void writeByteArray(byte[] b) {
        try {
            if (b.length <= 253) {
                if (!justCalc) {
                    buffer.put((byte) b.length);
                } else {
                    len += 1;
                }
            } else {
                if (!justCalc) {
                    buffer.put((byte) 254);
                    buffer.put((byte) b.length);
                    buffer.put((byte) (b.length >> 8));
                    buffer.put((byte) (b.length >> 16));
                } else {
                    len += 4;
                }
            }
            if (!justCalc) {
                buffer.put(b);
            } else {
                len += b.length;
            }
            int i = b.length <= 253 ? 1 : 4;
            while((b.length + i) % 4 != 0) {
                if (!justCalc) {
                    buffer.put((byte) 0);
                } else {
                    len += 1;
                }
                i++;
            }
        } catch (Exception e) {
            Log.e("tmessages", "write byte array error");
        }
    }

    public void writeDouble(double d) {
        try {
            writeInt64(Double.doubleToRawLongBits(d));
        } catch(Exception e) {
            Log.e("tmessages", "write double error");
        }
    }


    public int getIntFromByte(byte b) {
        return b >= 0 ? b : ((int)b) + 256;
    }

    public int length() {
        if (!justCalc) {
            return buffer.position();
        }
        return len;
    }

    public int getPosition() {
        return buffer.position();
    }

    public int readInt32(boolean exception) {
        try {
            return buffer.getInt();
        } catch (Exception e) {
            if (exception) {
                throw new RuntimeException("read int32 error", e);
            } else {
                Log.e("tmessages", "read int32 error");
            }
        }
        return 0;
    }

    public boolean readBool(boolean exception) {
        int consructor = readInt32(exception);
        if (consructor == 0x997275b5) {
            return true;
        } else if (consructor == 0xbc799737) {
            return false;
        }
        if (exception) {
            throw new RuntimeException("Not bool value!");
        } else {
            Log.e("tmessages", "Not bool value!");
        }
        return false;
    }

    public long readInt64(boolean exception) {
        try {
            return buffer.getLong();
        } catch (Exception e) {
            if (exception) {
                throw new RuntimeException("read int64 error", e);
            } else {
                Log.e("tmessages", "read int64 error");
            }
        }
        return 0;
    }


    public String readString(boolean exception) {
        try {
            int sl = 1;
            int l = getIntFromByte(buffer.get());
            if(l >= 254) {
                l = getIntFromByte(buffer.get()) | (getIntFromByte(buffer.get()) << 8) | (getIntFromByte(buffer.get()) << 16);
                sl = 4;
            }
            byte[] b = new byte[l];
            buffer.get(b);
            int i = sl;
            while((l + i) % 4 != 0) {
                buffer.get();
                i++;
            }
            return new String(b, "UTF-8");
        } catch (Exception e) {
            if (exception) {
                throw new RuntimeException("read string error", e);
            } else {
                Log.e("tmessages", "read string error");
            }
        }
        return null;
    }

    public byte[] readByteArray(boolean exception) {
        try {
            int sl = 1;
            int l = getIntFromByte(buffer.get());
            if (l >= 254) {
                l = getIntFromByte(buffer.get()) | (getIntFromByte(buffer.get()) << 8) | (getIntFromByte(buffer.get()) << 16);
                sl = 4;
            }
            byte[] b = new byte[l];
            buffer.get(b);
            int i = sl;
            while((l + i) % 4 != 0) {
                buffer.get();
                i++;
            }
            return b;
        } catch (Exception e) {
            if (exception) {
                throw new RuntimeException("read byte array error", e);
            } else {
                Log.e("tmessages", "read byte array error");
            }
        }
        return null;
    }


    public double readDouble(boolean exception) {
        try {
            return Double.longBitsToDouble(readInt64(exception));
        } catch(Exception e) {
            if (exception) {
                throw new RuntimeException("read double error", e);
            } else {
                Log.e("tmessages", "read double error");
            }
        }
        return 0;
    }

}
