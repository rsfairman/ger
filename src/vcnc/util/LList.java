package vcnc.util;

/*

My own implementation of linked lists since java.util.LinkedList isn't
*really* a linked list -- you can't get your hands on the tree nodes.
This is a doubly-linked list.

*/

import java.util.Collection;


public class LList<T> {
  
  // First and last nodes. 
  public LLNode<T> head = null;
  public LLNode<T> tail = null;
  
  
  public void add(T d) {
    
    // Add d to the end of the list.
    if (head == null)
      {
        // Brand new list.
        head = new LLNode<T>();
        head.data = d;
        tail = head;
        
        // Note that head/tail next/prev are still null.
        return;
      }
    
    if (tail.prev == null)
      {
        // Only the second item in the list.
        tail = new LLNode<T>();
        tail.data = d;
        
        head.next = tail;
        tail.prev = head;
        
        return;
      }
    
    // Got here, so normal case.
    LLNode<T> n = new LLNode<T>();
    n.data = d;
    
    tail.next = n;
    n.prev = tail;
    tail = n;
  }
  
  public void remove(LLNode<T> n) {
    
    // Assuming that n is a node in this linked list, remove it from the list.
    if (n.prev == null)
      {
        // Special case: n must be the head.
        head = n.next;
        
        // Careful: head might be null too if the list is now empty.
        if (head != null)
          head.prev = null;
        
        return;
      }
    
    if (n.next == null)
      {
        // Special case: n must be the tail.
        tail = n.prev;
        tail.next = null;
        return;
      }
    
    // Normal case.
    n.prev.next = n.next;
    n.next.prev = n.prev;
  }
  
  public LLNode<T> removeGet(LLNode<T> n) {
    
    // As above, but return the node that replaces the node that is removed.
    // Thus, if we have
    // head -> ... -> N1 -> N2 -> N3 -> ....
    // and we remove N2, then we end up with
    // head -> ... -> N1 -> N3 -> ....
    // and N3 is returned.
    //
    // NOTE: If n is the tail, then this returns null.
    remove(n);
    return n.next; 
  }
  
  public void insertAfter(LLNode<T> n,Collection<T> items) {
    
    // Make the given node (assumed to be valid and non-null) point to the 
    // first of the given items, then insert the remaining items into the list.
    // Note that this takes a list of T objects (the data itself), not existing 
    // nodes.
    // 
    // WARNING: This takes a Collection. If you want the items in a particular
    // order (as you typically would), then use an appropriate instance of this
    // interface, like ArrayList.

    // We want to insert between n and oldNext.
    LLNode<T> oldNext = null;
    if (n != tail)
      oldNext = n.next;
    
    LLNode<T> cur = n;
    for (T item : items) 
      { 
        LLNode<T> newNode = new LLNode<>();
        newNode.data = item;
        newNode.prev = cur;
        cur.next = newNode;
        
        cur = newNode;
      }
    
    // cur now points to the last newly added node.
    if (oldNext == null)
      // The new items were added at the tail.
      tail = cur;
    else
      {
        // The more normal case of insertion in the middle.
        cur.next = oldNext;
        oldNext.prev = cur;
      }
  }

  public void insertAfter(LLNode<T> n,T data) {
    
    // As above, but insert a single, newly allocated, item after n.
    LLNode<T> newItem = new LLNode<T>();
    newItem.data = data;
    LLNode<T> oldNext = n.next;
    n.next = newItem;
    newItem.prev = n;
    
    if (n != tail)
      {
        oldNext.prev = newItem;
        newItem.next = oldNext;
      }
    else
      tail = newItem;
  }
  
  public void insertAfter(LLNode<T> n,LLNode<T> start,LLNode<T> end) {
    
    // Insert the series of nodes from start to end, which themselves form
    // a portion of a linked list, after n.
    // 
    // BEWARE:
    // * It is assumed that start.next.next.next... will eventually
    //   reach end. There's no checking for that kind of error.
    // * This simply inserts the nodes from start to end into the list.
    //   It does NOT MAKE A COPY. That will often be wrong, but I couldn't
    //   see any easy way (is it impossible due to Java's type system?)
    //   to create a deep copy here of the data being inserted. Often, the
    //   user should create a complete duplicate of the original data before
    //   calling this method. Otherwise, if you use this to rearrange or
    //   duplicate portions of an existing list, then you're likely to get
    //   all kinds of weird circularity.
    //
    // BUG: Maybe one way to cut this problem off at the pass would be
    // to make the inserted bit an entire list -- an LList<T> in its own
    // right. Then it would be harder to accidentally forget the issues
    // above.
    //
    // Similar to the function above, based on a Collection.

    // We want to insert between n and oldNext (which may be null if n
    // was the tail).
    LLNode<T> oldNext = n.next;
    
    // Because we are not copying what is being inserted, this is easy.
    n.next = start;
    start.prev = n;
    
    if (oldNext != null)
      {
        end.next = oldNext;
        oldNext.prev = end;
      }
    else
      {
        // n was the tail.
        tail = end;
        end.next = null;
      }
  }
  
  public void replaceAt(LLNode<T> n,Collection<T> items) {
    
    // Replace the given node with the first of the given items, then insert 
    // the remaining items into the list. Note that this takes a list of
    // T objects (the data itself), not the nodes.
    // 
    // WARNING: This takes a Collection. If you want the items in a particular
    // order (as you typically would), then use an appropriate instance of this
    // interface, like ArrayList.
    
    // For brevity, use existing code, even though it's slightly less efficient.
    insertAfter(n,items);
    remove(n);
  }
  
  public LLNode<T> replaceAtGet(LLNode<T> n,Collection<T> items) {
    
    // As above, but return the node for the first of the newly
    // inserted items.
    insertAfter(n,items);
    return removeGet(n);
  }
  
  public void truncateAt(LLNode<T> n) {
    
    // Truncate the list just before the given node. So n, and everything
    // after it, is gone from the list.
    this.tail = n.prev;
    this.tail.next = null;
  }
  
  
}




