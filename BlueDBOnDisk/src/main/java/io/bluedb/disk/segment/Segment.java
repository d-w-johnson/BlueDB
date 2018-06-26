package io.bluedb.disk.segment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import io.bluedb.api.exceptions.BlueDbException;
import io.bluedb.api.keys.BlueKey;
import io.bluedb.api.keys.TimeFrameKey;
import io.bluedb.disk.serialization.BlueSerializer;

public class Segment <T extends Serializable> {

	private final BlueSerializer serializer;
	private final Path segmentPath;

	public Segment(Path segmentPath, BlueSerializer serializer) {
		this.segmentPath = segmentPath;
		this.serializer = serializer;
	}

	@Override
	public String toString() {
		return "<Segment for path " + segmentPath.toString() + ">";
	}

	public boolean contains(BlueKey key) throws BlueDbException {
		File file = getFileFor(key);
		if (!file.exists()) {
			return false;
		}
		List<BlueEntity<T>> entities = fetch(file);
		return contains(key, entities);
	}

	public void put(BlueKey key, T value) throws BlueDbException {
		File file = getFileFor(key);
		ArrayList<BlueEntity<T>> entities = fetch(file);
		BlueEntity<T> newEntity = new BlueEntity<T>(key, value);
		remove(key, entities);
		entities.add(newEntity);
		persist(file, entities);
	}

	public void delete(BlueKey key) throws BlueDbException {
		File file = getFileFor(key);
		if (file.exists()) {
			ArrayList<BlueEntity<T>> entities = fetch(file);
			remove(key, entities);
			persist(file, entities);
		}
	}

	public T get(BlueKey key) throws BlueDbException {
		File file = getFileFor(key);
		ArrayList<BlueEntity<T>> entities = fetch(file);
		return get(key, entities);
	}

	public List<T> getAll() throws BlueDbException {
		File[] filesInFolder = segmentPath.toFile().listFiles();
		List<T> results = new ArrayList<>();
		for (File file: filesInFolder) {
			for (BlueEntity<T> entity: fetch(file)) {
				results.add(entity.getObject());
			}
		}
		return results;
	}

    public List<BlueEntity<T>> getRange(long minTime, long maxTime) throws BlueDbException {
		File[] filesInFolder = segmentPath.toFile().listFiles();
		List<BlueEntity<T>> results = new ArrayList<>();
		for (File file: filesInFolder) {
			List<BlueEntity<T>> fileContents = fetch(file);
			for (BlueEntity<T> entity: fileContents) {
				BlueKey key = entity.getKey();
				if (inTimeRange(minTime, maxTime, key)) {
					results.add(entity);
				}
			}
		}
		return results;
	}

	protected File getFileFor(BlueKey key) {
		return getPathFor(key).toFile();
	}

	protected Path getPath() {
		return segmentPath;
	}

	private Path getPathFor(BlueKey key) {
		String fileName = String.valueOf(key.getGroupingNumber());
		return Paths.get(segmentPath.toString(), fileName);
	}

	// TODO handle locking?
	@SuppressWarnings("unchecked")
	private ArrayList<BlueEntity<T>> fetch(File file) throws BlueDbException {
		if (!file.exists())
			return new ArrayList<BlueEntity<T>>();
		byte[] fileData = load(file.toPath());
		ArrayList<BlueEntity<T>> fileContents =  (ArrayList<BlueEntity<T>>) serializer.deserializeObjectFromByteArray((fileData));
		return fileContents;
	}

	// TODO handle locking?
	private void persist(File file, ArrayList<BlueEntity<T>> entites) throws BlueDbException {
		if (entites.isEmpty()) {
			file.delete();
		} else {
			save(file.toPath(), entites);
		}
	}

	private static boolean inTimeRange(long minTime, long maxTime, BlueKey key) {
		if (key instanceof TimeFrameKey) {
			TimeFrameKey timeFrameKey = (TimeFrameKey) key;
			return timeFrameKey.getEndTime() >= minTime && timeFrameKey.getStartTime() <= maxTime;
		} else {
			return key.getGroupingNumber() >= minTime && key.getGroupingNumber() <= maxTime;
		}
	}
	
	protected static <T extends Serializable> T remove(BlueKey key, List<BlueEntity<T>> entities) {
		for (int i = 0; i < entities.size(); i++) {
			BlueEntity<T> entity = entities.get(i);
			if (entity.getKey().equals(key)) {
				entities.remove(i);
				return entity.getObject();
			}
		}
		return null;
	}

	protected static <T extends Serializable> boolean contains(BlueKey key, List<BlueEntity<T>> entities) {
		return get(key, entities) != null;
	}

	protected static <T extends Serializable> T get(BlueKey key, List<BlueEntity<T>> entities) {
		for (BlueEntity<T> entity: entities) {
			if (entity.getKey().equals(key)) {
				return entity.getObject();
			}
		}
		return null;
	}

	// TODO move to a FileManager class
	public byte[] load(Path path) throws BlueDbException {
		File file = path.toFile();
		if (!file.exists())
			return null;
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
			// TODO delete the file ?
			throw new BlueDbException("error writing to disk (" + path +")", e);
		}
	}

	// TODO move to a FileManager class
	public void save(Path path, Object o) throws BlueDbException {
		File file = path.toFile();
		file.getParentFile().mkdirs();
		byte[] bytes = serializer.serializeObjectToByteArray(o);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(bytes);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			// TODO delete the file
			throw new BlueDbException("error writing to disk (" + path +")", e);
		}
	}

	@Override
	public int hashCode() {
		return 31 + ((segmentPath == null) ? 0 : segmentPath.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Segment)) {
			return false;
		}
		Segment<?> other = (Segment<?>) obj;
		if (segmentPath == null) {
			return other.segmentPath == null;
		} else {
			return segmentPath.equals(other.segmentPath);
		}
	}
}
