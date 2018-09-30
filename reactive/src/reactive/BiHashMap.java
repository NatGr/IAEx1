package reactive;

import java.util.HashMap;
import java.util.Map;

/* Bidirectional Hashmap, used to get state number from state description
* adapted from https://stackoverflow.com/questions/1670038/does-java-have-a-hashmap-with-reverse-lookup */
class BiHashMap<KeyType, ValueType> {
	private Map<KeyType, ValueType> keyToValueMap = new HashMap<KeyType, ValueType>();
    private Map<ValueType, KeyType> valueToKeyMap = new HashMap<ValueType, KeyType>();
    
    public void put(KeyType key, ValueType value){
        keyToValueMap.put(key, value);
        valueToKeyMap.put(value, key);
    }
    
    public KeyType getKey(ValueType value){
        return valueToKeyMap.get(value);
    }

    public ValueType get(KeyType key){
        return keyToValueMap.get(key);
    }
}
