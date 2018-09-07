package io.bluedb.api.index;

import java.io.Serializable;
import java.util.List;
import io.bluedb.api.keys.BlueKey;

public interface KeyExtractor<K extends BlueKey, T extends Serializable> extends Serializable {
	public List<K> extractKeys(T object);
	public Class<K> getType();
}
