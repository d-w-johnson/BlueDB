package io.bluedb.disk;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.bluedb.api.BlueCollection;
import io.bluedb.api.BlueDb;
import io.bluedb.api.exceptions.BlueDbException;
import io.bluedb.disk.collection.BlueCollectionImpl;

public class BlueDbOnDisk implements BlueDb {

	private final Path path;
	
	private final Map<String, BlueCollectionImpl<? extends Serializable>> collections = new HashMap<>();
	
	BlueDbOnDisk(Path path, Class<?>...registeredSerializableClasses) {
		this.path = path;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> BlueCollection<T> getCollection(Class<T> type, String name) throws BlueDbException {
		synchronized (collections) {
			BlueCollectionImpl<T> collection = (BlueCollectionImpl<T>) collections.get(name);
			if(collection == null) {
				collection = new BlueCollectionImpl<>(this, type);
				collections.put(name, collection);
			} else if(!collection.getType().equals(type)) {
				throw new BlueDbException("The " + name + " collection already exists for a different type [collectionType=" + collection.getType() + " invalidType=" + type + "]");
			}
				
			return collection;
		}
	}

	@Override
	public void shutdown() throws BlueDbException {
		// TODO Auto-generated method stub
	}

	public Path getPath() {
		return path;
	}

	private void recover() {
		// TODO implement and also add to startup
	}
	
}
