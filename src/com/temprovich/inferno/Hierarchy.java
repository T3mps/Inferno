package com.temprovich.inferno;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Hierarchy implements Iterable<Entity> {
    
    private Member root;

    private Hierarchy(Entity entity) {
        this.root = new Member(entity);
    }

    private Hierarchy(Hierarchy hierarchy) {
        this.root = new Member(hierarchy.root);
    }

    public static final Hierarchy create(Entity root) {
        return new Hierarchy(root);
    }

    public static final Hierarchy create(Hierarchy hierarchy) {
        return new Hierarchy(hierarchy);
    }

    public Member defineRoot(Entity entity) {
        this.root = new Member(entity);
        return this.root;
    }

    public Member addChild(Entity child) {
        Member member = new Member(child);
        this.root.add(member);
        return member;
    }

    public Member addChild(Member parent, Entity child) {
        Member member = new Member(child);
        parent.add(member);
        return member;
    }

    public boolean has(Entity key) {
        return find(root, key);
    }

    public List<Entity> decendants(Entity key) {
        Member member = findMember(root, key);

        if (member == null) {
            return new ArrayList<Entity>();
        }

        return decendants(member);
    }

    private List<Entity> decendants(Member member) {
        List<Entity> decendants = new ArrayList<Entity>();

        for (Member child : member.getChildren()) {
            decendants.add(child.getEntity());
            decendants.addAll(decendants(child));
        }

        return decendants;
    }

    private boolean find(Member member, Entity key) {
        if (member.getEntity().equals(key)) {
            return true;
        }

        for (Member child : member.getChildren()) {
            if (find(child, key)) {
                return true;
            }
        }

        return false;
    }

    private Member findMember(Member member, Entity key) {
        if (member.getEntity().equals(key)) {
            return member;
        }
        
        for (Member child : member.getChildren()) {
            Member found = findMember(child, key);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public List<Member> preorderTraversal() {
        List<Member> members = new ArrayList<Member>();
        preorderTraversal(root, members);
        return members;
    }

    private void preorderTraversal(Member member, List<Member> members) {
        members.add(member);

        for (Member child : member.getChildren()) {
            preorderTraversal(child, members);
        }
    }

    public List<Member> postorderTraversal() {
        List<Member> members = new ArrayList<Member>();
        postorderTraversal(root, members);
        return members;
    }

    private void postorderTraversal(Member member, List<Member> members) {
        for (Member child : member.getChildren()) {
            postorderTraversal(child, members);
        }

        members.add(member);
    }

    public int size() {
        return decendantCount(root);
    }

    public int decendantCount(Entity key) {
        Member member = findMember(root, key);

        if (member == null) {
            return 0;
        }

        return decendantCount(member);
    }

    private int decendantCount(Member member) {
        int count = 1;
        
        for (Member child : member.getChildren()) {
            count += decendantCount(child);
        }

        return count;
    }

    public int height() {
        return height(root);
    }

    public int height(Entity key) {
        Member member = findMember(root, key);

        if (member == null) {
            return 0;
        }

        return height(member);
    }

    public int height(Member member) {
        int height = 0;
        
        for (Member child : member.getChildren()) {
            height = Math.max(height(child), height);
        }

        return height + 1;
    }

    public int depth(Entity key) {
        Member member = findMember(root, key);

        if (member == null) {
            return -1;
        }

        return depth(member);  
    }

    public int depth(Member member) {
        if (member == root) {
            return 0;
        }

        int depth = 0;

        for (Member parent = member.getParent(); parent != null; parent = parent.getParent()) {
            depth++;
        }

        return depth;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public Member getRoot() {
        return root;
    }

    public void setRoot(Member root) {
        this.root = root;
    }

    @Override
    public Iterator<Entity> iterator() {
        return new HierarchyIterator();
    }

    private class HierarchyIterator implements Iterator<Entity> {
        
        private List<Member> members = preorderTraversal();
        private int index = 0;
        
        @Override
        public boolean hasNext() {
            return index < members.size();
        }
        
        @Override
        public Entity next() {
            return members.get(index++).getEntity();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Member {

        private Entity entity;
        private List<Member> children;
        private Member parent;
        
    
        public Member(Entity entity) {
            this.entity = entity;
            this.children = new ArrayList<Member>();
            this.parent = null;
        }
    
        public Member(Member member) {
            this.entity = member.entity;
            this.children = new ArrayList<Member>();
            this.parent = member.parent;
        }
    
        public void add(Member member) {
            children.add(member);
            member.parent = this;
        }
    
        public void add(Entity entity) {
            Member member = new Member(entity);
            children.add(member);
            member.parent = this;
        }
    
        public void insert(int index, Member member) {
            children.add(index, member);
            member.parent = this;
        }
    
        public Member get(int index) {
            return children.get(index);
        }
    
        public void remove(Member member) {
            children.remove(member);
            
            if (member.parent != null) {
                member.parent.children.remove(member);
                member.parent = null;
            }
        }
    
        public void remove(int index) {
            Member member = children.remove(index);
            
            if (member != null) {
                member.parent = null;
            }
        }
    
        public void removeAll() {
            children.clear();
        }
    
        public int size() {
            return children.size();
        }
    
        public boolean hasChildren() {
            return !children.isEmpty();
        }
    
        public boolean hasParent() {
            return parent != null;
        }
    
        public Entity getEntity() {
            return entity;
        }
    
        public Member getParent() {
            return parent;
        }
    
        public List<Member> getChildren() {
            return children;
        }
    
        public boolean isRoot() {
            return parent == null;
        }
    
        public boolean isLeaf() {
            return children.isEmpty();
        }
    
        @Override
        public String toString() {
            return entity.toString();
        }
    }
}
