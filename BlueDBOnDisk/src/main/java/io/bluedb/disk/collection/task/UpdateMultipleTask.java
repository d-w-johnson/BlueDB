package io.bluedb.disk.collection.task;

import java.io.Serializable;
import java.util.List;

import io.bluedb.api.Condition;
import io.bluedb.api.Updater;
import io.bluedb.api.exceptions.BlueDbException;
import io.bluedb.api.keys.BlueKey;
import io.bluedb.disk.collection.BlueCollectionImpl;
import io.bluedb.disk.recovery.PendingChange;
import io.bluedb.disk.recovery.RecoveryManager;
import io.bluedb.disk.segment.BlueEntity;
import io.bluedb.disk.segment.Segment;

public class UpdateMultipleTask<T extends Serializable> implements Runnable {
	private final BlueCollectionImpl<T> collection;
	private final Updater<T> updater;
	private final long min;
	private final long max;
	private final  List<Condition<T>> conditions;
	
	
	public UpdateMultipleTask(BlueCollectionImpl<T> collection, long min, long max, List<Condition<T>> conditions, Updater<T> updater) {
		this.collection = collection;
		this.min = min;
		this.max = max;
		this.conditions = conditions;
		this.updater = updater;
	}

	@Override
	public void run() {
		try {
			List<BlueEntity<T>> entities = collection.findMatches(min, max, conditions);
			for (BlueEntity<T> entity: entities) {
				BlueKey key = entity.getKey();
				T value = (T) entity.getObject();
				RecoveryManager<T> recoveryManager = collection.getRecoveryManager();
				PendingChange<T> change = recoveryManager.saveUpdate(key, value, updater);
				List<Segment<T>> segments = collection.getSegmentManager().getAllSegments(key);
				for (Segment<T> segment: segments) {
					change.applyChange(segment);
				}
				collection.getRecoveryManager().removeChange(change);
				// TODO probably make it fail before doing any updates if any update fails?
			}
		} catch (BlueDbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "<UpdateMultipleTask [" + min + ", " + max + "] with " + conditions.size() + " conditions>";
	}
}
