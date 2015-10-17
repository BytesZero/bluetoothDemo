package com.zsl.bluetoothdemo.utils.ble;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by zsl on 15/9/24.
 * 广播报文的信息
 */
public class ParsedAd {
    //flags
    public byte flags;
    //ServerData uuid
    public String serverData_uuid;
    //ServerData data
    public String serverData_data;
    //loaclName
    public String localName;
    //manufacturer
    public short manufacturer;
    //UUID
    public List<UUID> uuids;

    public ParsedAd() {
        this.uuids = new ArrayList<UUID>();
    }

    @Override
    public String toString() {
        return "ParsedAd{" +
                "flags=" + flags +
                ", serverData_uuid=" + serverData_uuid +
                ", serverData_data='" + serverData_data + '\'' +
                ", localName='" + localName + '\'' +
                ", manufacturer=" + manufacturer +
                ", uuids=" + uuids +
                '}';
    }

    public static ParsedAd parseData(byte[] adv_data) {
        ParsedAd parsedAd = new ParsedAd();
        //把byte[]转换为一个byteBuffer(缓冲区)，然后按照c的排序使用LITTLE_ENDIAN，java默认是BIG_ENDIAN
        ByteBuffer buffer = ByteBuffer.wrap(adv_data).order(ByteOrder.LITTLE_ENDIAN);
        //buffer.remaining() 返回剩余的可用长度，如果小于2返回真
        while (buffer.remaining() > 2) {
            //获取此时buffer的长度
            byte length = buffer.get();
            if (length == 0)
                break;
            //获取类型，同时－1
            byte type = buffer.get();
            length -= 1;

            switch (type) {
                case 0x01: // 获得到Flags，同时－1
                    parsedAd.flags = buffer.get();
                    length--;
                    break;

                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getInt())));
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        parsedAd.uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    parsedAd.localName = new String(sb).trim();
                    break;
                case 0x16: //ServerData
                    //uuid
                    if (length>=2){
                        String sd_uuid=String.format("%02x", buffer.getShort());
                        parsedAd.serverData_uuid=sd_uuid;
                        length -= 2;
                    }
                    //data
                    String serverData="";
                    while (length >= 2) {
                        String sd_data=String.format("%04x", buffer.getShort());
                        serverData+=sd_data+":";
                        length -= 2;
                    }
                    parsedAd.serverData_data=serverData;
                    break;
                case (byte) 0xFF: // Manufacturer Specific Data
                    parsedAd.manufacturer = buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                //buffer.position() 相当于一个游标，记录我们从哪里开始写数据，或者标记我们从哪里开始读取数据
                buffer.position(buffer.position() + length);
            }
        }
        return parsedAd;
    }
}
