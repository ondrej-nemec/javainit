package common.structures;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import common.functions.Mapper;

public class MapDictionary<K, V> implements Dictionary<K> {
	
	private final Map<K, V> map;
	
	public static <K, V> MapDictionary<K, V> hashMap() {
		return new MapDictionary<>(new HashMap<>());
	}
	
	public static MapDictionary<Object, Object> properties() {
		return new MapDictionary<>(new Properties());
	}

	public MapDictionary(Map<K, V> map) {
		this.map = map;
	}
	
	@Override
	public Object getValue(K name) {
		return map.get(name);
	}
	
	public MapDictionary<K, V> put(K key, V value) {
		map.put(key, value);
		return this;
	}

	public MapDictionary<K, V> putAll(Map<K, V> values) {
		map.putAll(values);
		return this;
	}
	
	public V remove(K key) {
		return map.remove(key);
	}
	
	public Set<K> keySet() {
		return map.keySet();
	}
	
	public Collection<V> values() {
		return map.values();
	}
	
	public int size() {
		return map.size();
	}
	
	public Map<K, V> toMap() {
		return map;
	}
	
	public void forEach2(BiConsumer<K, V> action) {
		map.forEach(action);
	}
	
	public <E extends Throwable> void forEach(ThrowingBiConsumer<K, DictionaryValue, E> action) throws E {
		for (K k : map.keySet()) {
			action.accept(k, new DictionaryValue(map.get(k)));
		}
	}
	
	@Override
	public String toString() {
		return map.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( ! (obj instanceof MapDictionary) )
			return false;
		MapDictionary<?, ?> dictionary = (MapDictionary<?, ?>)obj;
		return map.equals(dictionary.map);
	}

	public <T> T parse(Class<T> clazz) {
		return parse(clazz, null);
	}

	public <T> T parse(Class<T> clazz, String key) {
		try {
			return Mapper.get().parse(clazz, map, key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
