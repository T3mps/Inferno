package com.temprovich.inferno.hierarchy;

import java.util.ArrayList;
import java.util.List;

import com.temprovich.inferno.Entity;

class Member {

    private Entity entity;
    private List<Member> children;
    private Member parent;
    

    Member(Entity entity) {
        this.entity = entity;
        this.children = new ArrayList<Member>();
        this.parent = null;
    }

    Member(Member member) {
        this.entity = member.entity;
        this.children = new ArrayList<Member>();
        this.parent = member.parent;
    }

    public void add(Member member) {
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
}
