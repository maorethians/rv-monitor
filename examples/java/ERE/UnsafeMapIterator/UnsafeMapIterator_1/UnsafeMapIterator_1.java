import java.util.*;

 public class UnsafeMapIterator_1 {
   public static void main(String[] args){
    try{
        Map<String, String> testMap = new HashMap<String,String>();
        testMap.put("Foo", "Bar");
        testMap.put("Bar", "Foo");
        Set<String> keys = testMap.keySet();
		mop.UnsafeMapIteratorRuntimeMonitor.createCollEvent(testMap, keys);
        Iterator i = keys.iterator();
		mop.UnsafeMapIteratorRuntimeMonitor.createIterEvent(keys,i);
        testMap.put("breaker", "borked");
		mop.UnsafeMapIteratorRuntimeMonitor.updateMapEvent(testMap);
		mop.UnsafeMapIteratorRuntimeMonitor.useIterEvent(i);
        System.out.println(i.next());
     }
     catch(Exception e){
        System.out.println("java found the problem too");
     }
   }
 }
