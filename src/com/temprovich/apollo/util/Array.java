package com.temprovich.apollo.util;

import java.util.Comparator;

public class Array<T> {

    private T[] array;
    private int size;

    public Array() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public Array(int initialCapacity) {
        this.array = (T[]) new Object[initialCapacity];
    }

    public Array(T[] array) {
        this.array = array;
        this.size = array.length;
    }

    public Array(Array<T> array) {
        this.array = array.array;
        this.size = array.size;
    }

    public void add(T value) {
        if (size == array.length) {
            resize(size << 1);
        }
        array[size++] = value;
    }

    public void add(int index, T value) {
        if (size == array.length) {
            resize(size << 1);
        }
        System.arraycopy(array, index, array, index + 1, size - index);
        array[index] = value;
        size++;
    }

    public void addAll(Array<T> array) {
        if (size + array.size > array.array.length) {
            resize(size + array.size);
        }
        System.arraycopy(array.array, 0, this.array, size, array.size);
        size += array.size;
    }

    public void addAll(T[] array) {
        if (size + array.length > this.array.length) {
            resize(size + array.length);
        }
        System.arraycopy(array, 0, this.array, size, array.length);
        size += array.length;
    }

    public void addAll(int index, Array<T> array) {
        if (size + array.size > array.array.length) {
            resize(size + array.size);
        }
        System.arraycopy(array.array, 0, this.array, index, array.size);
        size += array.size;
    }

    public void addAll(int index, T[] array) {
        if (size + array.length > this.array.length) {
            resize(size + array.length);
        }
        System.arraycopy(array, 0, this.array, index, array.length);
        size += array.length;
    }

    public void clear() {
        size = 0;
    }

    public T get(int index) {
        return array[index];
    }

    public Array<T> push(T value) {
        add(value);
        return this;
    }

    public T pop() {
        int index = size - 1;
        T t = array[index];
        array[--size] = null;
        return t;
    }

    public void set(int index, T value) {
        array[index] = value;
    }

    public void remove(int index) {
        System.arraycopy(array, index + 1, array, index, size - index - 1);
        size--;
    }

    public void remove(int index, int count) {
        System.arraycopy(array, index + count, array, index, size - index - count);
        size -= count;
    }

    public void remove(T t) {
        for (int i = 0; i < size; i++) {
            if (array[i] == t) {
                remove(i);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void resize(int newSize) {
        T[] newArray = (T[]) new Object[newSize];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }

    public int size() {
        return size;
    }

    public int length() {
        for (int i = size - 1; i >= 0; i--) {
            if (array[i] != null) {
                return i + 1;
            }
        }
        return 0;
    }

    public T[] toArray() {
        return array;
    }

    public Array<T> clone() {
        return new Array<T>(array);
    }

    public boolean contains(T value) {
        for (int i = 0; i < size; i++) {
            if (array[i] == value) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(T value) {
        for (int i = size - 1; i >= 0; i--) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public void sort(Comparator<T> comparator) {
        sort(0, size, comparator);
    }

    public void sort(int fromIndex, int toIndex, Comparator<T> comparator) {
        if (fromIndex < 0) throw new ArrayIndexOutOfBoundsException(fromIndex);
        if (toIndex > size)  throw new ArrayIndexOutOfBoundsException(toIndex);
        if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        
        for (int i = fromIndex; i < toIndex; i++) {
            for (int j = i + 1; j < toIndex; j++) {
                if (comparator.compare(array[i], array[j]) > 0) {
                    T t = array[i];
                    array[i] = array[j];
                    array[j] = t;
                }
            }
        }
    }

    public void reverse() {
        for (int i = 0; i < size / 2; i++) {
            T tmp = array[i];
            array[i] = array[size - i - 1];
            array[size - i - 1] = tmp;
        }
    }

    public void shuffle() {
        for (int i = 0; i < size; i++) {
            int j = (int) (Math.random() * size);
            T tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    public void shuffle(int times) {
        for (int i = 0; i < times; i++) {
            shuffle();
        }
    }
    
}
