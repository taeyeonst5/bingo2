package com.allen_chou.bingo;

public class Room {
    String id;
    String title;
    String uid;
    Member init;
    Member join;
    int status;

    public Room(){
    }

    public Room(String title, Member init) {
        this.title = title;
        this.init = init;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Member getInit() {
        return init;
    }

    public void setInit(Member init) {
        this.init = init;
    }

    public Member getJoin() {
        return join;
    }

    public void setJoin(Member join) {
        this.join = join;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
