package io.github.sac.codec;

import io.github.sac.codec.CodecEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

/**
 * <p>Created by AUT0CRAT on 2/2/18.
 */
public class BasicCodecEngine extends CodecEngine {
    @Override
    public byte[] encode(JSONObject data) {
        return data.toString().getBytes();
    }

    @Override
    public byte[] encode(String data) {
        return data.getBytes();
    }

    @Override
    public String decode(byte[] data) {
        return new String(data, Charset.defaultCharset());
    }
}
