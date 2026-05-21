package com.myy.weitutravel.chat.service.memory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;

@Service
public class KryoSerializer {

    private final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 8) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            // 注册需要序列化的类
            kryo.register(ArrayList.class);
            kryo.register(HashMap.class);
            kryo.register(LinkedList.class);
            // 设置不限制引用深度
            kryo.setReferences(true);
            kryo.setRegistrationRequired(false);
            return kryo;
        }
    };

    /**
     * 序列化对象为字节数组
     */
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }

        Kryo kryo = kryoPool.obtain();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            kryo.writeClassAndObject(output, obj);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Kryo序列化失败", e);
        } finally {
            kryoPool.free(kryo);
        }
    }

    /**
     * 反序列化字节数组为对象
     */
    public Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        Kryo kryo = kryoPool.obtain();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Input input = new Input(bais)) {
            return kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new RuntimeException("Kryo反序列化失败", e);
        } finally {
            kryoPool.free(kryo);
        }
    }

    /**
     * 序列化为Base64字符串
     */
    public String serializeToString(Object obj) {
        byte[] bytes = serialize(obj);
        return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    /**
     * 从Base64字符串反序列化
     */
    public Object deserializeFromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(str);
        return deserialize(bytes);
    }
}
