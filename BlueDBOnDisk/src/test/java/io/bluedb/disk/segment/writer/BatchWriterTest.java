package io.bluedb.disk.segment.writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.bluedb.api.exceptions.BlueDbException;
import io.bluedb.api.keys.BlueKey;
import io.bluedb.api.keys.LongKey;
import io.bluedb.disk.file.BlueObjectInput;
import io.bluedb.disk.file.BlueObjectOutput;
import io.bluedb.disk.recovery.IndividualChange;
import io.bluedb.disk.serialization.BlueEntity;

public class BatchWriterTest {
	
	private static BlueKey key1 = new LongKey(1);
	private static BlueKey key2 = new LongKey(2);
	private static BlueKey key3 = new LongKey(3);
//	private static BlueKey key4 = new LongKey(4);
	private static BlueKey key5 = new LongKey(5);
//	private static BlueKey key6 = new LongKey(6);
	private static BlueKey key7 = new LongKey(7);
	private static BlueKey key8 = new LongKey(8);
//	private static BlueKey key9 = new LongKey(9);
	private static BlueEntity<String> value1at1 = new BlueEntity<>(key1, "1");
//	private static BlueEntity<String> value2at2 = new BlueEntity<>(key2, "2");
	private static BlueEntity<String> value3at3 = new BlueEntity<>(key3, "3");
//	private static BlueEntity<String> value4at4 = new BlueEntity<>(key4, "4");
	private static BlueEntity<String> value5at5 = new BlueEntity<>(key5, "5");
//	private static BlueEntity<String> value6at6 = new BlueEntity<>(key6, "6");
	private static BlueEntity<String> value7at7 = new BlueEntity<>(key7, "7");
//	private static BlueEntity<String> value8at8 = new BlueEntity<>(key8, "8");
//	private static BlueEntity<String> value9at9 = new BlueEntity<>(key9, "9");
//	private static BlueEntity<String> value1bAt1 = new BlueEntity<>(key1, "1b");
	private static BlueEntity<String> value5bAt5 = new BlueEntity<>(key5, "5b");
//	private static BlueEntity<String> value9bAt9 = new BlueEntity<>(key9, "9b");
	private static IndividualChange<String> delete2 = new IndividualChange<>(key2, null, null);
	private static IndividualChange<String> delete3 = new IndividualChange<>(key3, null, null);
	private static IndividualChange<String> delete5 = new IndividualChange<>(key5, null, null);
	private static IndividualChange<String> delete8 = new IndividualChange<>(key8, null, null);

	private static IndividualChange<String> insert1 = new IndividualChange<>(key1, null, "1");

//	private static IndividualChange<String> update1bAt1 = new IndividualChange<>(key1, "1", "1b");
	private static IndividualChange<String> update5bAt5 = new IndividualChange<>(key5, "5", "5b");
//	private static IndividualChange<String> update9bAt9 = new IndividualChange<>(key9, "9", "9b");

	@Test
	public void testDeletes() throws Exception {
		List<BlueEntity<String>> initialValues = Arrays.asList(value3at3, value5at5, value7at7);
		BlueObjectInput<BlueEntity<String>> mockInput = createMockInput(initialValues);
		
		List<BlueEntity<String>> results = new ArrayList<>();
		BlueObjectOutput<BlueEntity<String>> mockOutput = createMockOutput(results);

		List<IndividualChange<String>> deletes2and3and5and8 = Arrays.asList(delete2, delete3, delete5, delete8);
		BatchWriter<String> batchDeletes2and3and5and8 = new BatchWriter<>(deletes2and3and5and8);
		batchDeletes2and3and5and8.process(mockInput, mockOutput);
		
		assertEquals(Arrays.asList(value7at7), results);
	}

	@Test
	public void testUpdateAndInsert() throws Exception {
		List<BlueEntity<String>> initialValues = Arrays.asList(value3at3, value5at5, value7at7);
		BlueObjectInput<BlueEntity<String>> mockInput = createMockInput(initialValues);
		
		List<BlueEntity<String>> results = new ArrayList<>();
		BlueObjectOutput<BlueEntity<String>> mockOutput = createMockOutput(results);

		List<IndividualChange<String>> insert1andUpdate5 = Arrays.asList(insert1, update5bAt5);
		BatchWriter<String> batchInsert1andUpdate5 = new BatchWriter<>(insert1andUpdate5);
		batchInsert1andUpdate5.process(mockInput, mockOutput);
		
		assertEquals(Arrays.asList(value1at1, value3at3, value5bAt5, value7at7), results);
	}

	//	@Test
//	public void testProcess() throws Exception {
//		List<String> outputs = new ArrayList<>();
//		BlueObjectOutput<String> mockOutput = createMockOutput(outputs);
//		assertEquals(0, outputs.size());
//		mockOutput.write("yo yo");
//		assertEquals(1, outputs.size());
//	}

	private static <T extends Serializable> BlueObjectInput<T> createMockInput(List<T> values) throws BlueDbException {
		final LinkedList<T> inputValues = new LinkedList<>(values);
		@SuppressWarnings("unchecked")
		BlueObjectInput<T> mockOutput = Mockito.mock(BlueObjectInput.class);
		Mockito.doAnswer((x) -> !inputValues.isEmpty()).when(mockOutput).hasNext();
		Mockito.doAnswer((x) -> inputValues.poll()).when(mockOutput).next();
		Mockito.doAnswer((x) -> inputValues.peek()).when(mockOutput).peek();
		return mockOutput;
	}

	private static <T extends Serializable> BlueObjectOutput<T> createMockOutput(List<T> results) throws BlueDbException {
		@SuppressWarnings("unchecked")
		BlueObjectOutput<T> mockOutput = Mockito.mock(BlueObjectOutput.class);
		Answer<T> mockAnswer = new Answer<T>() {
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				@SuppressWarnings("unchecked")
				T outputValue = (T) invocation.getArguments()[0];
				results.add(outputValue);
				return null;
			}
		};
		Mockito.doAnswer(mockAnswer).when(mockOutput).write(anyObject());
		return mockOutput;
	}
}
