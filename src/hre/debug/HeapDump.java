// -*- tab-width:2 ; indent-tabs-mode:nil -*-

package hre.debug;

import hre.io.PrefixPrintStream;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class contains methods that use reflection to dump arbitrary data structures.
 * 
 * @author <a href="mailto:sccblom@ewi.utwente.nl">Stefan Blom</a>
 */
public class HeapDump {

  public static boolean is_instance(Object o,Class ... base_classes){
    Class o_class=o.getClass();
    for(Class base:base_classes){
      if (base.isInstance(o)) return true; 
    }
    return false;
  }
  
  public static boolean subtype(Class c,Class ... base_classes){
    for(Class base:base_classes){
      for(Class i=c;i!=null;i=i.getSuperclass()){
        if (i==base) return true;
      }
    }
    return false;    
  }

  /**
   * Dump the given object tree.
   * 
   * @param tree The root of the tree that is to be printed.
   * @param base_classes A set of classes that are part of the tree and must be recursively explored.
   */
  public static void tree_dump(PrefixPrintStream out,Object tree,Class ... base_classes){
    tree_dump(out,new HashSet<Object>(),tree,base_classes);
  }
  private static void tree_dump(PrefixPrintStream out,Set<Object> visited,Object tree,Class ... base_classes){
    if (visited.contains(tree)) return;
    visited.add(tree);
    Class tree_class=tree.getClass();
    out.printf("<%s>\n",tree_class.getName());
    out.enter();
    out.prefixAdd("  ");
    for(Field field:tree_class.getDeclaredFields()){
      field.setAccessible(true);
      try {
        Object val=field.get(tree);
        if (subtype(field.getType(),base_classes)) {
          if (val==null) {
            out.printf("null field %s%n",field.getName());
          } else {
            tree_dump(out,visited,val,base_classes);
          }
        } else if (val!=null && val instanceof Collection) {
          for(Object i:(Collection)val){
            if (i!=null && is_instance(i,base_classes)){
              tree_dump(out,visited,i,base_classes);
            }
          }
        } else if (val!=null && val instanceof Map) {
          out.printf("<map %s>\n",field.getName());
          for(Object tmp :((Map)val).entrySet()){
            Entry e=(Entry)tmp;
            out.printf("<entry>%n");
            out.printf("<key>%n");
            tree_dump(out,visited,e.getKey(),base_classes);
            out.printf("</key>%n");
            out.printf("<value>%n");
            tree_dump(out,visited,e.getValue(),base_classes);
            out.printf("</value>%n");
            out.printf("</entry>%n");
          }
          out.printf("</map>\n");
        } else if (val !=null && val instanceof Object[]) {
          for(Object i:(Object[])val){
            if (i!=null && is_instance(i,base_classes)){
              tree_dump(out,visited,i,base_classes);
            }
          }
        } else {
          out.printf("skipping field %s%n",field.getName());
        }         
      } catch (IllegalAccessException e){
        out.printf("unreadable field %s%n",field.getName());
      }
    }
    out.leave();
    out.printf("</%s>%n",tree_class.getName());
  }
}
