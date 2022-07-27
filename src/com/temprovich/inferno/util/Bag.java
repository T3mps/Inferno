
package com.temprovich.inferno.util;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.Arrays;

public class Bag<E> implements Collection<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public E[] data;
    private int size;

    public Bag() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public Bag(int capacity) {
        this.data = (E[]) new Object[capacity];
    }

    @SuppressWarnings("unchecked")
    public boolean add(Object e) {
		if (size == data.length) {
            grow();
        }

        data[size++] = (E) e;
        return true;
	}

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c)  changed |= add(e);
        return changed;
    }

    public void set(int index, E e) {
		if (index >= data.length) grow(index * 2);
		size = Math.max(size, index + 1);
		data[index] = e;
	}

    public E get(int index) {
        return data[index];
    }

    @Override
    public boolean contains(Object e) {
        for (int i = 0; i < size; i++) {
            if (data[i].equals(e)) {
                return true;
            }
        }
 
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) if (!contains(o)) return false;
        return true;
    }

    public E remove(int index) {
        E e = data[index];
        data[index] = data[--size];
        data[size] = null;
        return e;
    }

    @Override
    public boolean remove(Object e) {
        for (int i = 0; i < size; i++) {
			E current = data[i];

			if (e == current) {
				data[i] = data[--size];
				data[size] = null;
				return true;
			}
		}

		return false; 
    }

    public E removeLast() {
        if (size == 0) {
            return null;
        }

        E e = data[--size];
        data[size] = null;
        
        return e;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (var o : c) {
            changed |= remove(o);
        }

        return changed;
    }

    @Override
    public void clear() {
		for (int i = 0; i < size; i++) {
            data[i] = null;
        }

		size = 0;
	}

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    private void grow() {
		int newCapacity = (data.length * 3) / 2 + 1;
		grow(newCapacity);
	}

	@SuppressWarnings("unchecked")
	private void grow(int newCapacity) {
		E[] oldData = data;
		data = (E[])new Object[newCapacity];
		System.arraycopy(oldData, 0, data, 0, oldData.length);
	}

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<E> iterator() {
        return new BagIterator();
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[size];
        System.arraycopy(data, 0, array, 0, size);
        return array;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        }

        System.arraycopy(data, 0, a, 0, size);
        
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    public Stream<E> stream() {
        return Stream.of(data).limit(size);
    }
    
    public Stream<E> parallelStream() {
        return stream().parallel();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.deepHashCode(data);
        result = prime * result + size;
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Bag)) {
            return false;
        }
        Bag<E> other = (Bag<E>) obj;
        if (!Arrays.deepEquals(data, other.data)) {
            return false;
        }
        if (size != other.size) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(data[i]);
        }

        sb.append("]");
        
        return sb.toString();
    }
    
    private class BagIterator implements Iterator<E> {

        /** Current position. */
        private int pointer;

        /** True if the current position is within bounds. */
        private boolean next;


        @Override
        public boolean hasNext() {
            return (pointer < size);
        }


        @Override
        public E next() {
            if (pointer == size) throw new NoSuchElementException("No more elements");

            E e = data[pointer++];
            next = true;

            return e;
        }


        @Override
        public void remove() {
            if (!next) throw new IllegalStateException("Attempting to remove an item from an empty bag");

            next = false;
            Bag.this.remove(--pointer);
        }
    }
}
