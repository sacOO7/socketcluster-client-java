package io.github.sac.codec;

import io.github.sac.codec.CodecEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Binary message packet code. It is based on <a href="https://github.com/SocketCluster/sc-codec-min-bin">min-bin codec</a>
 *
 * <p>Created by AUT0CRAT on 2/2/18.
 */
public class MessagePacketCodec extends CodecEngine {
    @Override
    public byte[] encode(JSONObject jsonObject) {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        ArrayList<Object> a = new ArrayList<>();
        Map<String, Object[]> map = new HashMap<>();

        encodePublish(jsonObject, map);
        encodeEmit(jsonObject, map);
        encodeResponse(jsonObject, map);

        try {
            JSONObject toEncode = new JSONObject(map);
            System.out.println("to encode : " + toEncode.toString());
            packer.packString(toEncode.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packer.toByteArray();
    }

    @Override
    public byte[] encode(String data) {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packString(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packer.toByteArray();
    }

    private void encodeResponse(JSONObject jsonObject, Map<String, Object[]> map) {
        String rid = jsonObject.optString("rid", null);
        if (rid == null) {
            return;
        }

        List<Object> a = new ArrayList<>();
        a.add(rid);
        a.add(jsonObject.optString("error"));
        String cid = jsonObject.optString("data");
        if (cid != null) {
            a.add(cid);
        }

        map.put("r", a.toArray());

        jsonObject.remove("rid");
        jsonObject.remove("error");
        jsonObject.remove("data");
    }


    private void encodeEmit(JSONObject jsonObject, Map<String, Object[]> map) {
        String event = jsonObject.optString("event", null);
        if (event == null) {
            return;
        }

        List<Object> a = new ArrayList<>();
        a.add(jsonObject.optString("event"));
        a.add(jsonObject.optString("data"));
        String cid = jsonObject.optString("cid");
        if (cid != null) {
            a.add(cid);
        }

        map.put("e", a.toArray());

        jsonObject.remove("event");
        jsonObject.remove("data");
        jsonObject.remove("cid");
    }

    private void encodePublish(JSONObject jsonObject, Map<String, Object[]> map) {
        String event = jsonObject.optString("event", null);
        if (event == null || !event.equals("#publish")) {
            return;
        }
        JSONObject dataObject = jsonObject.optJSONObject("data");

        List<Object> a = new ArrayList<>();
        a.add(dataObject.optString("channel"));
        a.add(dataObject);
        String cid = jsonObject.optString("cid");
        if (cid != null) {
            a.add(cid);
        }
        map.put("p", a.toArray());

        jsonObject.remove("event");
        jsonObject.remove("data");
        jsonObject.remove("cid");

    }

    @Override
    public String decode(byte[] data) {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        try {
            ImmutableValue mapValue = unpacker.unpackValue();
            try {
                JSONObject object = new JSONObject(mapValue.toString());
                decodeEmit(object);
                decodePublish(object);
                decodeResponse(object);
                return object.toString();
            } catch (JSONException e) {
                return mapValue.toString();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void decodePublish(JSONObject object) {
        JSONArray p = object.optJSONArray("p");
        if (p == null) {
            return;
        }

        try {
            object.put("event", "#publish");
            JSONObject data = new JSONObject();
            int size = p.length();
            data.put("channel", p.get(0));
            data.put("data", p.get(1));
            if (size > 2) {
                data.put("cid", p.get(2));
            }
            object.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        object.remove("p");
    }

    private void decodeEmit(JSONObject object) {
        JSONArray e = object.optJSONArray("e");
        if (e == null) {
            return;
        }

        try {
            object.put("event", e.get(0));
            object.put("data", e.get(1));

            if (e.length() > 2) {
                object.put("cid", e.get(2));
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        object.remove("e");
    }

    private void decodeResponse(JSONObject object) {
        JSONArray r = object.optJSONArray("r");
        if (r == null) {
            return;
        }

        try {
            object.put("rid", r.get(0));
            object.put("error", r.get(1));
            object.put("data", r.get(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        object.remove("r");
    }
}
