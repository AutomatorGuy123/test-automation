package com.automation.common.converters;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import datainstiller.data.DataValueConverter;

public class MapEntryConverter implements DataValueConverter {

    @Override
    public <T> T fromString(String str, Class<T> cls) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public boolean canConvert(Class clazz) {
        return AbstractMap.class.isAssignableFrom(clazz);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        @SuppressWarnings("unchecked")
        AbstractMap<String, String> map = (AbstractMap<String, String>) value;
        for (Entry<String, String> entry : map.entrySet()) {
            writer.startNode(entry.getKey().toString());
            writer.setValue(entry.getValue().toString());
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> map = new HashMap<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            map.put(reader.getNodeName(), reader.getValue());
            reader.moveUp();
        }

        return map;
    }

}
