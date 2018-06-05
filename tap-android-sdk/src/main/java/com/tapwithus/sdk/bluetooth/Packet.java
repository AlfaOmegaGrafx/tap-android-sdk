package com.tapwithus.sdk.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Packet {

    private static final int BYTE_SIZE = 8;

    private byte[] data;
    private boolean lsb;

    private StringBuilder binaryDataStringBuilder = new StringBuilder();

    public Packet(byte[] data, int size) {
        this(data, size, true);
    }

    public Packet(byte[] data, int size, boolean lsb) {
        initData(data, size);
        this.lsb = lsb;
    }

    private void initData(byte[] data, int size) {
        byte[] d = data.clone();
        if (d.length > size) {
            d = Arrays.copyOfRange(d, 0, size);
        }
        if (d.length < size) {
            d = concat(d, new byte[size - d.length]);
        }
        setData(d);
    }

    public String getBinaryDataString() {
        return binaryDataStringBuilder.toString();
    }

    private void setData(byte[] data) {
        // Set the binaryDataStringBuilder based on the given data
        binaryDataStringBuilder.setLength(0);
        for (byte b: data) {
            String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF))
                    .replace(' ', '0');
            binaryDataStringBuilder.append(binaryString);
        }
        this.data = data;
    }

    public String getString(int start, int size) {
        if (lsb) {
            return getLsbString(start, size);
        } else {
            return getMsbString(start, size);
        }
    }

    private String getLsbString(int start, int size) {
        return getChunkStringFromBinaryDataStringBuilder(start, size);
    }

    private String getMsbString(int start, int size) {
        return binaryDataStringBuilder.substring(start, start + size);
    }

    public void set(int start, int size, String data) {
        if (lsb) {
            updateLsbData(start, size, data);
        } else {
            updateMsbData(start, size, data);
        }
    }

    private void updateLsbData(int start, int size, String data) {
        int byteNumber = start / BYTE_SIZE;
        String reversedByte = reverse(getByteStringFromBinaryDataStringBuilder(byteNumber));
        int startBit = start - byteNumber * BYTE_SIZE;

        String backToNormalByte = reverse(replaceRange(reversedByte, startBit, startBit + size, reverse(data)));
        binaryDataStringBuilder.replace(byteNumber * BYTE_SIZE, byteNumber * BYTE_SIZE + BYTE_SIZE, backToNormalByte);

        this.data = binaryStringToByteArray(binaryDataStringBuilder.toString());
    }

    private void updateMsbData(int startIndex, int size, String data) {
        binaryDataStringBuilder.replace(startIndex, startIndex + size, data);

        this.data = binaryStringToByteArray(binaryDataStringBuilder.toString());
    }

    private byte[] binaryStringToByteArray(String binaryString) {
        String[] binaryByteStrings = chunk(binaryString);
        byte[] byteArray = new byte[binaryByteStrings.length];
        for (int i = 0; i < binaryByteStrings.length; i++) {
            byteArray[i] = binaryByteStringToByte(binaryByteStrings[i]);
        }
        return byteArray;
    }

    private byte binaryByteStringToByte(String binaryByteString) {
        return (byte) Integer.parseInt(binaryByteString, 2);
    }

    private int byteToInt(byte b) {
        return b & 0xFF;
    }

    private byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;

        byte[] c = new byte[aLen + bLen];

        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    private String getByteStringFromBinaryDataStringBuilder(int byteNumber) {
        return chunk(binaryDataStringBuilder.toString())[byteNumber];
    }

    private String getChunkStringFromBinaryDataStringBuilder(int beginIndex, int endIndex) {
        String reversedBinaryString = reverseBinaryString(binaryDataStringBuilder.toString());
        int binaryStringLength = reversedBinaryString.length();
        int reversedBeginIndex = binaryStringLength - endIndex;
        int reveredEndIndex = binaryStringLength - beginIndex;

        return reversedBinaryString.substring(reversedBeginIndex - 1, reveredEndIndex);
    }

    private String reverseBinaryString(String binaryString) {
        String[] chunks = chunk(binaryString);
        String[] reversedChunks = reverseBytes(chunks);

        StringBuilder reversedBinaryString = new StringBuilder();
        for (String chunk: reversedChunks) {
            reversedBinaryString.append(chunk);
        }
        return reversedBinaryString.toString();
    }

    private String[] reverseBytes(String[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            String temp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = temp;
        }
        return bytes;
    }

    private String[] chunk(String string) {

        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < string.length()) {
            parts.add(string.substring(index, Math.min(index + BYTE_SIZE, string.length())));
            index += BYTE_SIZE;
        }

        return parts.toArray(new String[0]);
    }

    private String reverse(String string) {
        return new StringBuffer(string).reverse().toString();
    }

    private String replaceRange(String source, int startIndex, int endIndex, String replacement) {
        return new StringBuffer(source).replace(startIndex, endIndex, replacement).toString();
    }

    public class PacketValue {

        private int startIndex;
        private int size;
        private boolean unsigned;

        public PacketValue(int startBit, int size) {
            this.startIndex = startBit;
            this.size = size;
        }

        public String getString() {
            return Packet.this.getString(startIndex, size);
        }

        public boolean getBoolean() {
            return Packet.this.getString(startIndex, size).equals("1");
        }

        public int getInt() {
            String binaryString = Packet.this.getString(startIndex, size);
            return (int) (byte) Integer.parseInt(binaryString, 2);
        }

        public int getUnsignedInt() {
            String binaryString = Packet.this.getString(startIndex, size);
            return Integer.parseInt(binaryString, 2);
        }

        public long getLong() {
            String binaryString = Packet.this.getString(startIndex, size);
            return (long) (byte) Long.parseLong(binaryString, 2);
        }

        public long getUnsignedLong() {
            String binaryString = Packet.this.getString(startIndex, size);
            return Long.parseLong(binaryString, 2);
        }

        public void set(boolean data) {
            Packet.this.set(startIndex, size, toNumericalString(data));
        }

        public void set(int data) {
            Packet.this.set(startIndex, size, toBinaryString(data, size));
        }

        public void set(String data) {
            if (isNumerical(data)) {
                Packet.this.set(startIndex, size, data);
            }
        }

        private String toNumericalString(boolean data) {
            return data ? "1" : "0";
        }

        private String toBinaryString(int num, int size) {
            int intValue = num;
            if (intValue < 0) {
                intValue += 256;
            }
            return String
                    .format("%" + size + "s", Integer.toBinaryString(intValue))
                    .replace(' ', '0');
        }

        private boolean isNumerical(String s) {
            return s != null && s.matches("[-+]?\\d*\\.?\\d+");
        }
    }
}
